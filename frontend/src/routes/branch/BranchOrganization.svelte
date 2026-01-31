<script lang="ts">
  import { onMount } from 'svelte';
  import type { Organization, OrgTree as OrgTreeType } from '../../types/api';
  import { apiClient } from '../../lib/api';
  import OrgTreeNode from '../../components/OrgTreeNode.svelte';
  import { tabStore } from '../../lib/tabStore';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Badge } from '$lib/components/ui/badge';
  import { Separator } from '$lib/components/ui/separator';
  
  // Organization hierarchy types
  const ORG_TYPE_ORDER = ['DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR'];
  const ORG_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: '총판',
    AGENCY: '대리점',
    DEALER: '딜러',
    SELLER: '판매점',
    VENDOR: '가맹점'
  };
  
  const STATUS_LABELS: Record<string, string> = {
    ACTIVE: '정상',
    SUSPENDED: '정지',
    TERMINATED: '해지'
  };
  
  let organizations = $state<OrgTreeType[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let searchQuery = $state('');
  let selectedOrg = $state<OrgTreeType | null>(null);
  
  // Statistics
  let stats = $derived({
    total: countNodes(organizations),
    byType: countByType(organizations),
    byStatus: countByStatus(organizations)
  });
  
  function countNodes(nodes: OrgTreeType[]): number {
    return nodes.reduce((acc, node) => {
      return acc + 1 + (node.children ? countNodes(node.children) : 0);
    }, 0);
  }
  
  function countByType(nodes: OrgTreeType[]): Record<string, number> {
    const counts: Record<string, number> = {};
    function traverse(nodeList: OrgTreeType[]) {
      for (const node of nodeList) {
        counts[node.orgType] = (counts[node.orgType] || 0) + 1;
        if (node.children) traverse(node.children);
      }
    }
    traverse(nodes);
    return counts;
  }
  
  function countByStatus(nodes: OrgTreeType[]): Record<string, number> {
    const counts: Record<string, number> = {};
    function traverse(nodeList: OrgTreeType[]) {
      for (const node of nodeList) {
        counts[node.status] = (counts[node.status] || 0) + 1;
        if (node.children) traverse(node.children);
      }
    }
    traverse(nodes);
    return counts;
  }
  
  function buildTree(flatOrgs: Organization[]): OrgTreeType[] {
    const orgMap = new Map<string, OrgTreeType>();
    const roots: OrgTreeType[] = [];
    
    flatOrgs.forEach(org => {
      orgMap.set(org.id, { ...org, children: [] });
    });
    
    flatOrgs.forEach(org => {
      const node = orgMap.get(org.id)!;
      const pathSegments = org.orgCode.split('.');
      
      if (pathSegments.length === 1) {
        roots.push(node);
      } else {
        const parentPath = pathSegments.slice(0, -1).join('.');
        const parent = flatOrgs.find(o => o.orgCode === parentPath);
        if (parent) {
          const parentNode = orgMap.get(parent.id);
          if (parentNode) {
            parentNode.children.push(node);
          }
        }
      }
    });
    
    // Sort by orgType order
    function sortNodes(nodes: OrgTreeType[]): OrgTreeType[] {
      return nodes
        .sort((a, b) => {
          const aOrder = ORG_TYPE_ORDER.indexOf(a.orgType);
          const bOrder = ORG_TYPE_ORDER.indexOf(b.orgType);
          if (aOrder !== bOrder) return aOrder - bOrder;
          return a.name.localeCompare(b.name);
        })
        .map(node => ({
          ...node,
          children: node.children ? sortNodes(node.children) : []
        }));
    }
    
    return sortNodes(roots);
  }
  
  async function loadOrganizations() {
    loading = true;
    error = null;
    try {
      // Get the root organization first
      const rootResponse = await apiClient.get<Organization>('/organizations/root');
      if (!rootResponse.success || !rootResponse.data) {
        error = rootResponse.error?.message || 'Failed to load root organization';
        return;
      }
      
      const rootId = rootResponse.data.id;
      
      // Get all descendants
      const response = await apiClient.get<Organization[]>(
        `/organizations/${rootId}/descendants`
      );
      
      if (response.success && response.data) {
        organizations = buildTree(response.data);
      } else {
        error = response.error?.message || 'Failed to load organizations';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load organizations';
    } finally {
      loading = false;
    }
  }
  
  function handleSelect(org: OrgTreeType) {
    selectedOrg = org;
  }
  
  function handleOpenDetail(org: OrgTreeType) {
    tabStore.openTab({
      id: `branch-${org.id}`,
      title: org.name,
      icon: '🏢',
      component: 'BranchDetail',
      closeable: true,
      props: { branchId: org.id }
    });
  }
  
  function handleRefresh() {
    loadOrganizations();
  }
  
  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case 'ACTIVE': return 'default';
      case 'SUSPENDED': return 'secondary';
      case 'TERMINATED': return 'destructive';
      default: return 'outline';
    }
  }
  
  // Filter organizations by search query
  let filteredOrganizations = $derived.by(() => {
    if (!searchQuery.trim()) return organizations;
    
    const query = searchQuery.toLowerCase().trim();
    
    function filterNode(node: OrgTreeType): OrgTreeType | null {
      const matches = 
        node.name.toLowerCase().includes(query) ||
        node.orgCode.toLowerCase().includes(query);
      
      const filteredChildren = node.children
        ?.map(child => filterNode(child))
        .filter((child): child is OrgTreeType => child !== null) || [];
      
      if (matches || filteredChildren.length > 0) {
        return { ...node, children: filteredChildren };
      }
      return null;
    }
    
    return organizations
      .map(node => filterNode(node))
      .filter((node): node is OrgTreeType => node !== null);
  });
  
  onMount(() => {
    loadOrganizations();
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1600px] mx-auto">
  <!-- Header -->
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">조직 구성</h1>
      <span class="text-sm text-muted-foreground font-medium">
        총 {stats.total.toLocaleString()}개 조직
      </span>
    </div>
    <Button variant="outline" onclick={handleRefresh} disabled={loading}>
      <svg class="w-4 h-4 mr-2 {loading ? 'animate-spin' : ''}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 12a9 9 0 11-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
        <path d="M21 3v5h-5" />
      </svg>
      새로고침
    </Button>
  </header>

  <!-- Statistics Cards -->
  <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
    {#each ORG_TYPE_ORDER as type}
      <Card class="bg-gradient-to-br from-muted/50 to-muted/20">
        <CardContent class="p-4">
          <div class="text-xs text-muted-foreground font-medium uppercase tracking-wide mb-1">
            {ORG_TYPE_LABELS[type]}
          </div>
          <div class="text-2xl font-bold text-foreground">
            {(stats.byType[type] || 0).toLocaleString()}
          </div>
        </CardContent>
      </Card>
    {/each}
    <Card class="bg-gradient-to-br from-green-500/10 to-green-500/5 border-green-500/20">
      <CardContent class="p-4">
        <div class="text-xs text-green-600 font-medium uppercase tracking-wide mb-1">
          정상
        </div>
        <div class="text-2xl font-bold text-green-600">
          {(stats.byStatus['ACTIVE'] || 0).toLocaleString()}
        </div>
      </CardContent>
    </Card>
  </div>

  <!-- Main Content -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <!-- Tree View -->
    <div class="lg:col-span-2">
      <Card class="h-[calc(100vh-360px)] min-h-[500px] flex flex-col">
        <CardHeader class="pb-4 border-b border-border">
          <div class="flex items-center justify-between gap-4">
            <CardTitle class="text-lg">조직 계층도</CardTitle>
            <div class="flex items-center gap-2">
              <div class="relative">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
                </svg>
                <Input
                  type="text"
                  placeholder="조직명, 코드로 검색..."
                  value={searchQuery}
                  oninput={(e) => searchQuery = e.currentTarget.value}
                  class="pl-9 w-64"
                />
              </div>
            </div>
          </div>
        </CardHeader>
        <CardContent class="flex-1 overflow-y-auto p-4">
          {#if loading}
            <div class="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground">
              <div class="w-8 h-8 border-3 border-border border-t-primary rounded-full animate-spin"></div>
              <span>조직 정보를 불러오는 중...</span>
            </div>
          {:else if error}
            <div class="flex flex-col items-center justify-center h-full gap-4 text-destructive">
              <svg class="w-12 h-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 8v4m0 4h.01"/>
              </svg>
              <span class="font-medium">{error}</span>
              <Button variant="outline" onclick={handleRefresh}>다시 시도</Button>
            </div>
          {:else if filteredOrganizations.length === 0}
            <div class="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground">
              <svg class="w-12 h-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
              </svg>
              <span class="font-medium">
                {searchQuery ? '검색 결과가 없습니다' : '등록된 조직이 없습니다'}
              </span>
            </div>
          {:else}
            {#each filteredOrganizations as org (org.id)}
              <OrgTreeNode
                node={org}
                level={0}
                selectedId={selectedOrg?.id || null}
                onSelect={handleSelect}
              />
            {/each}
          {/if}
        </CardContent>
      </Card>
    </div>

    <!-- Detail Panel -->
    <div class="lg:col-span-1">
      <Card class="h-[calc(100vh-360px)] min-h-[500px] flex flex-col">
        <CardHeader class="pb-4 border-b border-border">
          <CardTitle class="text-lg">조직 상세</CardTitle>
        </CardHeader>
        <CardContent class="flex-1 overflow-y-auto p-4">
          {#if selectedOrg}
            <div class="flex flex-col gap-6">
              <!-- Basic Info -->
              <div class="flex flex-col gap-3">
                <div class="flex items-center gap-3">
                  <span class="w-10 h-10 rounded-lg bg-primary/10 text-primary text-sm font-bold flex items-center justify-center">
                    {ORG_TYPE_LABELS[selectedOrg.orgType]?.charAt(0) || '?'}
                  </span>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-semibold text-foreground truncate">{selectedOrg.name}</h3>
                    <p class="text-sm text-muted-foreground font-mono">{selectedOrg.orgCode}</p>
                  </div>
                </div>
                <div class="flex gap-2">
                  <Badge variant="secondary">{ORG_TYPE_LABELS[selectedOrg.orgType]}</Badge>
                  <Badge variant={getStatusVariant(selectedOrg.status)}>
                    {STATUS_LABELS[selectedOrg.status] || selectedOrg.status}
                  </Badge>
                </div>
              </div>

              <Separator />

              <!-- Details -->
              <div class="flex flex-col gap-4">
                <div class="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span class="text-muted-foreground">조직 유형</span>
                    <p class="font-medium text-foreground mt-1">{ORG_TYPE_LABELS[selectedOrg.orgType]}</p>
                  </div>
                  <div>
                    <span class="text-muted-foreground">상태</span>
                    <p class="font-medium text-foreground mt-1">{STATUS_LABELS[selectedOrg.status] || selectedOrg.status}</p>
                  </div>
                  <div>
                    <span class="text-muted-foreground">조직 코드</span>
                    <p class="font-medium font-mono text-foreground mt-1">{selectedOrg.orgCode}</p>
                  </div>
                  <div>
                    <span class="text-muted-foreground">하위 조직</span>
                    <p class="font-medium text-foreground mt-1">{selectedOrg.children?.length || 0}개</p>
                  </div>
                </div>
                
                {#if selectedOrg.children && selectedOrg.children.length > 0}
                  <div>
                    <span class="text-muted-foreground text-sm">하위 조직 목록</span>
                    <div class="mt-2 flex flex-wrap gap-1.5">
                      {#each selectedOrg.children.slice(0, 10) as child}
                        <Badge 
                          variant="outline" 
                          class="cursor-pointer hover:bg-muted transition-colors"
                          onclick={() => handleSelect(child)}
                        >
                          {child.name}
                        </Badge>
                      {/each}
                      {#if selectedOrg.children.length > 10}
                        <Badge variant="outline">+{selectedOrg.children.length - 10}</Badge>
                      {/if}
                    </div>
                  </div>
                {/if}
              </div>

              <Separator />

              <!-- Actions -->
              <div class="flex flex-col gap-2">
                <Button class="w-full" onclick={() => selectedOrg && handleOpenDetail(selectedOrg)}>
                  상세 정보 보기
                </Button>
              </div>
            </div>
          {:else}
            <div class="flex flex-col items-center justify-center h-full gap-4 text-muted-foreground">
              <svg class="w-12 h-12 opacity-50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"/>
              </svg>
              <span class="text-sm">왼쪽 트리에서 조직을 선택하세요</span>
            </div>
          {/if}
        </CardContent>
      </Card>
    </div>
  </div>
</div>
