<script lang="ts">
  import { onMount } from 'svelte';
  import { merchantApi } from '../../lib/merchantApi';
  import { tabStore } from '../../lib/tabStore';
  import {
    type MerchantDto,
    type MerchantListParams,
    MerchantStatus,
    MERCHANT_BUSINESS_TYPE_LABELS
  } from '../../types/merchant';
  import { BusinessType } from '../../types/branch';
  import { format } from 'date-fns';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Badge } from '$lib/components/ui/badge';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';

  let merchants = $state<MerchantDto[]>([]);
  let loading = $state(false);
  let initialLoading = $state(true);
  let hasMore = $state(true);
  let page = $state(0);
  const pageSize = 20;
  let totalCount = $state(0);

  let searchQuery = $state('');
  let businessTypeFilter = $state<BusinessType | ''>('');
  let statusFilter = $state<MerchantStatus | ''>('');

  let sentinelEl: HTMLDivElement;

  const STATUS_LABELS: Record<MerchantStatus, string> = {
    [MerchantStatus.ACTIVE]: '정상',
    [MerchantStatus.SUSPENDED]: '정지',
    [MerchantStatus.TERMINATED]: '해지'
  };

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case MerchantStatus.ACTIVE:
        return 'default';
      case MerchantStatus.SUSPENDED:
        return 'secondary';
      case MerchantStatus.TERMINATED:
        return 'destructive';
      default:
        return 'outline';
    }
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd');
    } catch {
      return '-';
    }
  }

  function formatBusinessNumber(num?: string): string {
    if (!num) return '-';
    const digits = num.replace(/\D/g, '');
    if (digits.length !== 10) return num;
    return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
  }

  async function loadMerchants(reset: boolean = false) {
    if (loading) return;
    if (!reset && !hasMore) return;

    loading = true;

    if (reset) {
      page = 0;
      merchants = [];
      hasMore = true;
    }

    try {
      const params: MerchantListParams = {
        page,
        size: pageSize,
        businessType: businessTypeFilter || undefined,
        status: statusFilter || undefined,
        search: searchQuery.trim() || undefined
      };

      const response = await merchantApi.getMerchants(params);

      if (response.success && response.data) {
        const newData = response.data.content || [];
        if (reset) {
          merchants = newData;
        } else {
          merchants = [...merchants, ...newData];
        }
        totalCount = response.data.totalElements || 0;
        
        const isLastPage = newData.length < pageSize;
        const apiHasNext = response.data.hasNext;
        hasMore = apiHasNext === true && !isLastPage;
        
        if (newData.length > 0) {
          page++;
        }
      } else {
        hasMore = false;
      }
    } catch (err) {
      console.error('Failed to load merchants:', err);
      hasMore = false;
    } finally {
      loading = false;
      initialLoading = false;
    }
  }

  function handleSearch() {
    loadMerchants(true);
  }

  function handleReset() {
    searchQuery = '';
    businessTypeFilter = '';
    statusFilter = '';
    loadMerchants(true);
  }

  function handleRowClick(merchant: MerchantDto) {
    tabStore.openTab({
      id: `merchant-${merchant.id}`,
      title: merchant.name,
      icon: '🏪',
      component: 'MerchantDetail',
      closeable: true,
      props: { merchantId: merchant.id }
    });
  }

  function handleNewMerchant() {
    tabStore.openTab({
      id: 'merchant-registration',
      title: '가맹점 등록',
      icon: '➕',
      component: 'MerchantRegistration',
      closeable: true
    });
  }

  onMount(() => {
    loadMerchants(true);
  });

  $effect(() => {
    if (!sentinelEl) return;
    if (initialLoading) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore && merchants.length > 0) {
          loadMerchants(false);
        }
      },
      { rootMargin: '100px', threshold: 0 }
    );

    observer.observe(sentinelEl);

    return () => observer.disconnect();
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1400px] mx-auto">
  <!-- Header -->
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">가맹점 목록</h1>
      <span class="text-sm text-muted-foreground font-medium">총 {totalCount.toLocaleString()}개</span>
    </div>
    <Button onclick={handleNewMerchant}>
      <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 5v14m-7-7h14"/>
      </svg>
      가맹점 등록
    </Button>
  </header>

  <!-- Filter Bar -->
  <div class="bg-muted/50 border border-border rounded-xl p-5">
    <div class="flex flex-wrap gap-4 mb-4">
      <!-- Search -->
      <div class="flex flex-col gap-1.5 flex-1 min-w-60">
        <Label for="search">검색어</Label>
        <div class="relative flex items-center">
          <svg class="absolute left-3 w-4 h-4 text-muted-foreground pointer-events-none" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
          </svg>
          <Input
            id="search"
            type="text"
            placeholder="가맹점코드, 상호, 대표자, 사업자번호..."
            value={searchQuery}
            oninput={(e) => searchQuery = e.currentTarget.value}
            onkeydown={(e) => e.key === 'Enter' && handleSearch()}
            class="pl-9"
          />
        </div>
      </div>

      <!-- Business Type Filter -->
      <div class="flex flex-col gap-1.5">
        <Label for="businessType">사업자유형</Label>
        <select
          id="businessType"
          bind:value={businessTypeFilter}
          class="h-10 px-3 pr-8 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring min-w-[140px]"
        >
          <option value="">전체</option>
          {#each Object.values(BusinessType) as type}
            <option value={type}>{MERCHANT_BUSINESS_TYPE_LABELS[type]}</option>
          {/each}
        </select>
      </div>

      <!-- Status Filter -->
      <div class="flex flex-col gap-1.5">
        <Label for="status">상태</Label>
        <select
          id="status"
          bind:value={statusFilter}
          class="h-10 px-3 pr-8 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring min-w-[140px]"
        >
          <option value="">전체</option>
          {#each Object.values(MerchantStatus) as status}
            <option value={status}>{STATUS_LABELS[status]}</option>
          {/each}
        </select>
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-3 border-t border-border">
      <Button variant="outline" onclick={handleReset}>초기화</Button>
      <Button onclick={handleSearch}>조회</Button>
    </div>
  </div>

  <!-- Table Container -->
  <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
    <Table>
       <TableHeader>
         <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
           <TableHead class="w-[140px] font-bold text-xs uppercase tracking-wide">가맹점코드</TableHead>
           <TableHead class="min-w-[160px] font-bold text-xs uppercase tracking-wide">상호</TableHead>
           <TableHead class="w-[140px] font-bold text-xs uppercase tracking-wide">영업점</TableHead>
           <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">담당자</TableHead>
           <TableHead class="w-[120px] font-bold text-xs uppercase tracking-wide">연락처</TableHead>
           <TableHead class="w-[130px] font-bold text-xs uppercase tracking-wide">사업자번호</TableHead>
           <TableHead class="w-[100px] text-center font-bold text-xs uppercase tracking-wide">유형</TableHead>
           <TableHead class="w-[80px] text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
           <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">등록일</TableHead>
         </TableRow>
       </TableHeader>
      <TableBody>
         {#if initialLoading}
           <!-- Loading Skeleton -->
           {#each Array(8) as _}
             <TableRow>
               <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
               <TableCell><div class="h-4 w-32 bg-muted animate-pulse rounded"></div></TableCell>
               <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
               <TableCell><div class="h-4 w-16 bg-muted animate-pulse rounded"></div></TableCell>
               <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
               <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
               <TableCell class="text-center"><div class="h-4 w-16 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
               <TableCell class="text-center"><div class="h-4 w-12 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
               <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
             </TableRow>
           {/each}
         {:else if merchants.length === 0}
           <!-- Empty State -->
           <TableRow>
             <TableCell colspan={9} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M3 21h18M3 7v1a3 3 0 003 3h12a3 3 0 003-3V7M3 7l2-4h14l2 4"/>
                  <path d="M5 21V11h14v10"/>
                  <path d="M10 21v-6h4v6"/>
                </svg>
                <p class="text-base font-semibold text-muted-foreground">검색 결과가 없습니다</p>
                <p class="text-sm text-muted-foreground/70">다른 검색 조건을 시도해 보세요</p>
              </div>
            </TableCell>
          </TableRow>
        {:else}
          <!-- Data Rows -->
          {#each merchants as merchant (merchant.id)}
            <TableRow 
              class="cursor-pointer hover:bg-primary/5 transition-colors even:bg-muted/30"
              onclick={() => handleRowClick(merchant)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(merchant)}
              role="button"
              tabindex={0}
            >
              <TableCell>
                <span class="font-mono text-sm text-primary font-medium">{merchant.merchantCode}</span>
              </TableCell>
              <TableCell>
                <span class="font-medium text-foreground">{merchant.name}</span>
              </TableCell>
               <TableCell>
                 <span class="text-sm text-muted-foreground">{merchant.organizationName || '-'}</span>
               </TableCell>
               <TableCell>
                 <span class="text-sm">{merchant.primaryContact?.name || '-'}</span>
               </TableCell>
               <TableCell>
                 <span class="text-sm text-muted-foreground">{merchant.primaryContact?.phone || '-'}</span>
               </TableCell>
               <TableCell>
                 <span class="font-mono text-sm">{formatBusinessNumber(merchant.businessNumber)}</span>
               </TableCell>
              <TableCell class="text-center">
                {#if merchant.businessType}
                  <Badge variant="secondary" class="text-xs">
                    {MERCHANT_BUSINESS_TYPE_LABELS[merchant.businessType]}
                  </Badge>
                {:else}
                  <span class="text-muted-foreground">-</span>
                {/if}
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(merchant.status)}>
                  {STATUS_LABELS[merchant.status] || merchant.status}
                </Badge>
              </TableCell>
              <TableCell>
                {formatDate(merchant.createdAt)}
              </TableCell>
            </TableRow>
          {/each}
        {/if}
      </TableBody>
    </Table>

    <!-- Loading More Indicator -->
    {#if loading && !initialLoading}
      <div class="flex items-center justify-center gap-3 py-6 text-muted-foreground text-sm">
        <div class="w-5 h-5 border-2 border-muted border-t-primary rounded-full animate-spin"></div>
        <span>더 불러오는 중...</span>
      </div>
    {/if}

    <!-- Infinite Scroll Sentinel -->
    <div bind:this={sentinelEl} class="h-px invisible"></div>

    <!-- End of List -->
    {#if !hasMore && merchants.length > 0 && !loading}
      <div class="py-5 text-center text-sm text-muted-foreground border-t border-border">
        모든 데이터를 불러왔습니다
      </div>
    {/if}
  </div>
</div>
