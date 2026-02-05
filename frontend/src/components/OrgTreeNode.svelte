<script lang="ts">
  import { untrack } from 'svelte';
  import type { OrgTree } from '../types/api';
  import { Badge } from '$lib/components/ui/badge';
  import OrgTreeNode from './OrgTreeNode.svelte';
  
  interface Props {
    node: OrgTree;
    level?: number;
    selectedId?: string | null;
    onSelect: (node: OrgTree) => void;
  }
  
  let {
    node,
    level = 0,
    selectedId = null,
    onSelect
  }: Props = $props();
  
  // Intentionally capture initial level value for default expansion state
  const defaultExpanded = untrack(() => level < 2);
  let expanded = $state(defaultExpanded);
  
  const orgTypeIcons: Record<string, string> = {
    DISTRIBUTOR: 'D',
    AGENCY: 'A',
    DEALER: 'De',
    SELLER: 'S',
    VENDOR: 'V'
  };
  
  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' {
    if (status === 'ACTIVE') return 'default';
    if (status === 'SUSPENDED') return 'secondary';
    return 'destructive';
  }
  
  function toggleExpand(e: Event) {
    e.stopPropagation();
    expanded = !expanded;
  }
  
  function handleSelect() {
    onSelect(node);
  }
  
  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleSelect();
    }
  }
  
  const hasChildren = $derived(node.children && node.children.length > 0);
  const isSelected = $derived(selectedId === node.id);
</script>

<div class="select-none">
  <div
    class="flex items-center gap-2 p-2 cursor-pointer rounded transition-colors hover:bg-muted
      {isSelected ? 'bg-primary/10 border-l-3 border-primary' : ''}"
    style="padding-left: {level * 20 + 8}px"
    onclick={handleSelect}
    onkeydown={handleKeyDown}
    role="button"
    tabindex="0"
  >
    {#if hasChildren}
      <button
        class="w-5 h-5 p-0 bg-transparent border-none cursor-pointer text-xs text-muted-foreground hover:text-foreground flex items-center justify-center"
        onclick={toggleExpand}
        aria-label={expanded ? 'Collapse' : 'Expand'}
      >
        {expanded ? 'v' : '>'}
      </button>
    {:else}
      <span class="w-5 inline-block"></span>
    {/if}
    
    <span class="w-6 h-6 rounded bg-primary/10 text-primary text-xs font-bold flex items-center justify-center">
      {orgTypeIcons[node.orgType] || '?'}
    </span>
    <span class="font-medium text-foreground">{node.name}</span>
    <span class="text-muted-foreground text-sm hidden sm:inline">({node.orgCode})</span>
    <Badge variant="outline" class="text-xs px-1.5">
      <svg class="w-3 h-3 mr-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
      {node.merchantCount ?? 0}
    </Badge>
    <Badge variant={getStatusVariant(node.status)} class="ml-auto text-xs">
      {node.status}
    </Badge>
  </div>
  
  {#if hasChildren && expanded}
    {#each node.children as child (child.id)}
      <OrgTreeNode
        node={child}
        level={level + 1}
        {selectedId}
        {onSelect}
      />
    {/each}
  {/if}
</div>
