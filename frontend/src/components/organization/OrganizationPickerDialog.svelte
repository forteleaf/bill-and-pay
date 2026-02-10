<script lang="ts">
  import { Dialog as DialogPrimitive } from "bits-ui";
  
  import type { Organization, OrgTree as OrgTreeType } from '@/types/api';
  import { apiClient } from '@/api/client';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Badge } from '$lib/components/ui/badge';
  import { cn } from '$lib/utils';

  interface Props {
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
    onSelect?: (org: Organization) => void;
    selectedId?: string | null;
  }

  let {
    open = $bindable(false),
    onOpenChange,
    onSelect,
    selectedId = null
  }: Props = $props();

  let organizations = $state<OrgTreeType[]>([]);
  let flatOrganizations = $state<Organization[]>([]);
  let loading = $state(false);
  let error = $state<string | null>(null);
  let searchQuery = $state('');
  let currentSelectedId = $state<string | null>(null);
  
  // Sync currentSelectedId with prop when it changes
  $effect(() => {
    currentSelectedId = selectedId;
  });

  const ORG_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: '총판',
    AGENCY: '대리점',
    DEALER: '딜러',
    SELLER: '셀러',
    VENDOR: '벤더'
  };

  const ORG_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
    DISTRIBUTOR: { bg: 'bg-indigo-100', text: 'text-indigo-700' },
    AGENCY: { bg: 'bg-violet-100', text: 'text-violet-700' },
    DEALER: { bg: 'bg-blue-100', text: 'text-blue-700' },
    SELLER: { bg: 'bg-cyan-100', text: 'text-cyan-700' },
    VENDOR: { bg: 'bg-emerald-100', text: 'text-emerald-700' }
  };

  function buildTree(flatOrgs: Organization[]): OrgTreeType[] {
    const orgMap = new Map<string, OrgTreeType>();
    const roots: OrgTreeType[] = [];

    flatOrgs.forEach(org => {
      orgMap.set(org.id, { ...org, children: [] });
    });

    flatOrgs.forEach(org => {
      const node = orgMap.get(org.id)!;
      const pathSegments = org.path.split('.');

      if (pathSegments.length === 1) {
        roots.push(node);
      } else {
        const parentPath = pathSegments.slice(0, -1).join('.');
        const parent = flatOrgs.find(o => o.path === parentPath);
        if (parent) {
          const parentNode = orgMap.get(parent.id);
          if (parentNode) parentNode.children.push(node);
        }
      }
    });

    return roots;
  }

  async function loadOrganizations() {
    loading = true;
    error = null;
    try {
      const rootResponse = await apiClient.get<Organization>('/organizations/root');
      if (!rootResponse.success || !rootResponse.data) {
        error = rootResponse.error?.message || '조직 정보를 불러올 수 없습니다';
        return;
      }

      const rootId = rootResponse.data.id;
      const response = await apiClient.get<Organization[]>(`/organizations/${rootId}/descendants`);

      if (response.success && response.data) {
        flatOrganizations = response.data;
        organizations = buildTree(response.data);
      } else {
        error = response.error?.message || '조직 정보를 불러올 수 없습니다';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : '조직 정보를 불러올 수 없습니다';
    } finally {
      loading = false;
    }
  }

  let filteredOrganizations = $derived.by(() => {
    if (!searchQuery.trim()) return organizations;
    
    const query = searchQuery.toLowerCase().trim();
    
    function filterTree(nodes: OrgTreeType[]): OrgTreeType[] {
      return nodes.reduce<OrgTreeType[]>((acc, node) => {
        const matchesSearch = 
          node.name.toLowerCase().includes(query) ||
          node.orgCode.toLowerCase().includes(query) ||
          ORG_TYPE_LABELS[node.orgType]?.toLowerCase().includes(query);

        const filteredChildren = node.children ? filterTree(node.children) : [];

        if (matchesSearch || filteredChildren.length > 0) {
          acc.push({
            ...node,
            children: filteredChildren
          });
        }

        return acc;
      }, []);
    }

    return filterTree(organizations);
  });

  function handleSelect(org: Organization) {
    currentSelectedId = org.id;
  }

  function handleConfirm() {
    if (currentSelectedId) {
      const selected = flatOrganizations.find(o => o.id === currentSelectedId);
      if (selected) {
        onSelect?.(selected);
        open = false;
        onOpenChange?.(false);
      }
    }
  }

  function handleCancel() {
    currentSelectedId = selectedId;
    open = false;
    onOpenChange?.(false);
  }

  $effect(() => {
    if (open && organizations.length === 0) {
      loadOrganizations();
    }
  });

  $effect(() => {
    currentSelectedId = selectedId;
  });
</script>

{#snippet TreeNode(node: OrgTreeType, level: number = 0)}
  {@const colors = ORG_TYPE_COLORS[node.orgType] || { bg: 'bg-gray-100', text: 'text-gray-700' }}
  {@const isSelected = currentSelectedId === node.id}
  {@const hasChildren = node.children && node.children.length > 0}
  
  <div class="select-none">
    <button
      type="button"
      class={cn(
        "w-full flex items-center gap-2 px-3 py-2 rounded-lg transition-all text-left",
        "hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1",
        isSelected && "bg-primary/10 ring-2 ring-primary"
      )}
      style="padding-left: {level * 16 + 12}px"
      onclick={() => handleSelect(node)}
    >
      {#if hasChildren}
        <span class="w-4 h-4 flex items-center justify-center text-muted-foreground text-xs">
          ▼
        </span>
      {:else}
        <span class="w-4"></span>
      {/if}
      
      <span class={cn("w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold", colors.bg, colors.text)}>
        {ORG_TYPE_LABELS[node.orgType]?.charAt(0) || '?'}
      </span>
      
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span class="font-medium text-foreground truncate">{node.name}</span>
          <span class="text-xs text-muted-foreground font-mono hidden sm:inline">({node.orgCode})</span>
        </div>
        <span class={cn("text-xs", colors.text)}>{ORG_TYPE_LABELS[node.orgType]}</span>
      </div>
      
      {#if isSelected}
        <svg class="w-5 h-5 text-primary shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 6L9 17l-5-5"/>
        </svg>
      {/if}
    </button>
    
    {#if hasChildren}
      <div class="ml-4">
        {#each node.children as child (child.id)}
          {@render TreeNode(child, level + 1)}
        {/each}
      </div>
    {/if}
  </div>
{/snippet}

<DialogPrimitive.Root open={open} onOpenChange={(v) => { open = v; onOpenChange?.(v); }}>
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay 
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" 
    />
    <DialogPrimitive.Content
      class="fixed left-[50%] top-[50%] z-50 w-full max-w-2xl max-h-[85vh] translate-x-[-50%] translate-y-[-50%] rounded-2xl border bg-background shadow-2xl duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] flex flex-col"
    >
      <div class="flex items-center justify-between px-6 py-4 border-b border-border shrink-0">
        <div>
          <DialogPrimitive.Title class="text-lg font-bold text-foreground">
            영업점 선택
          </DialogPrimitive.Title>
          <DialogPrimitive.Description class="text-sm text-muted-foreground mt-1">
            가맹점이 소속될 영업점을 선택하세요
          </DialogPrimitive.Description>
        </div>
        <DialogPrimitive.Close class="rounded-full p-2 hover:bg-muted transition-colors">
          <svg class="w-5 h-5 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </DialogPrimitive.Close>
      </div>

      <div class="px-6 py-3 border-b border-border shrink-0">
        <div class="relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
          <Input
            type="text"
            placeholder="영업점명, 코드, 유형으로 검색..."
            value={searchQuery}
            oninput={(e) => searchQuery = e.currentTarget.value}
            class="pl-10"
          />
        </div>
      </div>

      <div class="flex-1 overflow-y-auto px-6 py-4 min-h-0">
        {#if loading}
          <div class="flex flex-col items-center justify-center h-64 gap-4 text-muted-foreground">
            <div class="w-10 h-10 border-4 border-muted border-t-primary rounded-full animate-spin"></div>
            <span class="font-medium">조직 정보를 불러오는 중...</span>
          </div>
        {:else if error}
          <div class="flex flex-col items-center justify-center h-64 gap-4 text-destructive">
            <svg class="w-12 h-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4m0 4h.01"/>
            </svg>
            <span class="font-medium">{error}</span>
            <Button variant="outline" size="sm" onclick={loadOrganizations}>다시 시도</Button>
          </div>
        {:else if filteredOrganizations.length === 0}
          <div class="flex flex-col items-center justify-center h-64 gap-4 text-muted-foreground">
            <svg class="w-12 h-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
            <span class="font-medium">검색 결과가 없습니다</span>
          </div>
        {:else}
          <div class="space-y-1">
            {#each filteredOrganizations as org (org.id)}
              {@render TreeNode(org, 0)}
            {/each}
          </div>
        {/if}
      </div>

      <div class="flex items-center justify-between px-6 py-4 border-t border-border bg-muted/30 shrink-0">
        {#if currentSelectedId}
          {@const selected = flatOrganizations.find(o => o.id === currentSelectedId)}
          {#if selected}
            <div class="flex items-center gap-2 text-sm">
              <span class="text-muted-foreground">선택:</span>
              <Badge variant="secondary">{selected.name}</Badge>
            </div>
          {/if}
        {:else}
          <div></div>
        {/if}
        <div class="flex gap-3">
          <Button variant="outline" onclick={handleCancel}>취소</Button>
          <Button onclick={handleConfirm} disabled={!currentSelectedId}>선택</Button>
        </div>
      </div>
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
</DialogPrimitive.Root>
