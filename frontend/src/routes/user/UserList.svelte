<script lang="ts">
  import { onMount } from 'svelte';
  import { userApi } from '@/api/user';
  import {
    type UserDto,
    type UserListParams,
    UserStatus,
    UserRole,
    USER_STATUS_LABELS,
    USER_ROLE_LABELS
  } from '@/types/user';
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

  interface Props {
    onselect?: (userId: string) => void;
    onregister?: () => void;
  }

  let { onselect, onregister }: Props = $props();

  let users = $state<UserDto[]>([]);
  let loading = $state(false);
  let initialLoading = $state(true);
  let hasMore = $state(true);
  let page = $state(0);
  const pageSize = 20;
  let totalCount = $state(0);

  let searchQuery = $state('');
  let statusFilter = $state<UserStatus | ''>('');
  let roleFilter = $state<UserRole | ''>('');

  let sentinelEl: HTMLDivElement;

  function getStatusVariant(status: UserStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case UserStatus.ACTIVE:
        return 'default';
      case UserStatus.SUSPENDED:
        return 'secondary';
      case UserStatus.DELETED:
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

  async function loadUsers(reset: boolean = false) {
    if (loading) return;
    if (!reset && !hasMore) return;

    loading = true;

    if (reset) {
      page = 0;
      users = [];
      hasMore = true;
    }

    try {
      const params: UserListParams = {
        page,
        size: pageSize,
        status: statusFilter || undefined,
        role: roleFilter || undefined,
        search: searchQuery.trim() || undefined
      };

      const response = await userApi.getUsers(params);

      if (response.success && response.data) {
        const newData = response.data.content || [];
        if (reset) {
          users = newData;
        } else {
          users = [...users, ...newData];
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
      console.error('Failed to load users:', err);
      hasMore = false;
    } finally {
      loading = false;
      initialLoading = false;
    }
  }

  function handleSearch() {
    loadUsers(true);
  }

  function handleReset() {
    searchQuery = '';
    statusFilter = '';
    roleFilter = '';
    loadUsers(true);
  }

  function handleRowClick(user: UserDto) {
    onselect?.(user.id);
  }

  function handleNewUser() {
    onregister?.();
  }

  onMount(() => {
    loadUsers(true);
  });

  $effect(() => {
    if (!sentinelEl) return;
    if (initialLoading) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore && users.length > 0) {
          loadUsers(false);
        }
      },
      { rootMargin: '100px', threshold: 0 }
    );

    observer.observe(sentinelEl);

    return () => observer.disconnect();
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1400px] mx-auto">
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">사용자 목록</h1>
      <span class="text-sm text-muted-foreground font-medium">총 {totalCount.toLocaleString()}명</span>
    </div>
    <Button onclick={handleNewUser}>
      <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 5v14m-7-7h14"/>
      </svg>
      사용자 등록
    </Button>
  </header>

  <div class="bg-muted/50 border border-border rounded-xl p-5">
    <div class="flex flex-wrap gap-4 mb-4">
      <div class="flex flex-col gap-1.5 flex-1 min-w-60">
        <Label for="search">검색어</Label>
        <div class="relative flex items-center">
          <svg class="absolute left-3 w-4 h-4 text-muted-foreground pointer-events-none" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
          </svg>
          <Input
            id="search"
            type="text"
            placeholder="이름, 아이디로 검색..."
            value={searchQuery}
            oninput={(e) => searchQuery = e.currentTarget.value}
            onkeydown={(e) => e.key === 'Enter' && handleSearch()}
            class="pl-9"
          />
        </div>
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="status">상태</Label>
        <select
          id="status"
          bind:value={statusFilter}
          class="h-10 px-3 pr-8 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring min-w-[140px]"
        >
          <option value="">전체</option>
          {#each Object.values(UserStatus) as status}
            <option value={status}>{USER_STATUS_LABELS[status]}</option>
          {/each}
        </select>
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="role">역할</Label>
        <select
          id="role"
          bind:value={roleFilter}
          class="h-10 px-3 pr-8 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring min-w-[140px]"
        >
          <option value="">전체</option>
          {#each Object.values(UserRole) as role}
            <option value={role}>{USER_ROLE_LABELS[role]}</option>
          {/each}
        </select>
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-3 border-t border-border">
      <Button variant="outline" onclick={handleReset}>초기화</Button>
      <Button onclick={handleSearch}>조회</Button>
    </div>
  </div>

  <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
    <Table>
      <TableHeader>
        <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
          <TableHead class="min-w-[120px] font-bold text-xs uppercase tracking-wide">이름</TableHead>
          <TableHead class="w-[140px] font-bold text-xs uppercase tracking-wide">아이디</TableHead>
          <TableHead class="min-w-[180px] font-bold text-xs uppercase tracking-wide">이메일</TableHead>
          <TableHead class="w-[100px] text-center font-bold text-xs uppercase tracking-wide">역할</TableHead>
          <TableHead class="min-w-[140px] font-bold text-xs uppercase tracking-wide">소속 영업점</TableHead>
          <TableHead class="w-[80px] text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
          <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">등록일</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {#if initialLoading}
          {#each Array(8) as _}
            <TableRow>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-36 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-16 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-28 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-12 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
            </TableRow>
          {/each}
        {:else if users.length === 0}
          <TableRow>
            <TableCell colspan={7} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                <p class="text-base font-semibold text-muted-foreground">검색 결과가 없습니다</p>
                <p class="text-sm text-muted-foreground/70">다른 검색 조건을 시도해 보세요</p>
              </div>
            </TableCell>
          </TableRow>
        {:else}
          {#each users as user (user.id)}
            <TableRow 
              class="cursor-pointer hover:bg-primary/5 transition-colors even:bg-muted/30"
              onclick={() => handleRowClick(user)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(user)}
              role="button"
              tabindex={0}
            >
              <TableCell>
                <span class="font-medium text-foreground">{user.fullName}</span>
              </TableCell>
              <TableCell>
                <span class="font-mono text-sm text-primary">{user.username}</span>
              </TableCell>
              <TableCell>
                <span class="text-sm text-muted-foreground">{user.email}</span>
              </TableCell>
              <TableCell class="text-center">
                <Badge variant="secondary" class="text-xs">
                  {USER_ROLE_LABELS[user.role]}
                </Badge>
              </TableCell>
              <TableCell>
                <span class="text-sm text-muted-foreground">{user.orgPath || '-'}</span>
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(user.status)}>
                  {USER_STATUS_LABELS[user.status]}
                </Badge>
              </TableCell>
              <TableCell>
                {formatDate(user.createdAt)}
              </TableCell>
            </TableRow>
          {/each}
        {/if}
      </TableBody>
    </Table>

    {#if loading && !initialLoading}
      <div class="flex items-center justify-center gap-3 py-6 text-muted-foreground text-sm">
        <div class="w-5 h-5 border-2 border-muted border-t-primary rounded-full animate-spin"></div>
        <span>더 불러오는 중...</span>
      </div>
    {/if}

    <div bind:this={sentinelEl} class="h-px invisible"></div>

    {#if !hasMore && users.length > 0 && !loading}
      <div class="py-5 text-center text-sm text-muted-foreground border-t border-border">
        모든 데이터를 불러왔습니다
      </div>
    {/if}
  </div>
</div>
