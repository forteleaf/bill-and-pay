<script lang="ts">
  import type { OrgTree } from '../types/api';
  
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
  
  let expanded = $state(level < 2);
  
  const orgTypeIcons: Record<string, string> = {
    DISTRIBUTOR: '🏢',
    AGENCY: '🏪',
    DEALER: '🏬',
    SELLER: '🏦',
    VENDOR: '🏭'
  };
  
  const statusColors: Record<string, string> = {
    ACTIVE: 'bg-green-100 text-green-800',
    SUSPENDED: 'bg-yellow-100 text-yellow-800',
    TERMINATED: 'bg-red-100 text-red-800'
  };
  
  function toggleExpand(e: Event) {
    e.stopPropagation();
    expanded = !expanded;
  }
  
  function handleSelect() {
    onSelect(node);
  }
  
  const hasChildren = $derived(node.children && node.children.length > 0);
  const isSelected = $derived(selectedId === node.id);
</script>

<div class="org-tree-node">
  <div
    class="node-content"
    class:selected={isSelected}
    style="padding-left: {level * 20}px"
    onclick={handleSelect}
    role="button"
    tabindex="0"
  >
    {#if hasChildren}
      <button
        class="expand-icon"
        onclick={toggleExpand}
        aria-label={expanded ? 'Collapse' : 'Expand'}
      >
        {expanded ? '▼' : '▶'}
      </button>
    {:else}
      <span class="expand-placeholder"></span>
    {/if}
    
    <span class="org-icon">{orgTypeIcons[node.orgType] || '📁'}</span>
    <span class="org-name">{node.name}</span>
    <span class="org-code">({node.orgCode})</span>
    <span class="status-badge {statusColors[node.status] || ''}">{node.status}</span>
  </div>
  
  {#if hasChildren && expanded}
    {#each node.children as child (child.id)}
      <svelte:self
        node={child}
        level={level + 1}
        {selectedId}
        {onSelect}
      />
    {/each}
  {/if}
</div>

<style>
  .org-tree-node {
    user-select: none;
  }
  
  .node-content {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem;
    cursor: pointer;
    border-radius: 0.25rem;
    transition: background-color 0.2s;
  }
  
  .node-content:hover {
    background-color: #f3f4f6;
  }
  
  .node-content.selected {
    background-color: #dbeafe;
    border-left: 3px solid #3b82f6;
  }
  
  .expand-icon {
    width: 1.25rem;
    height: 1.25rem;
    padding: 0;
    background: none;
    border: none;
    cursor: pointer;
    font-size: 0.75rem;
    color: #6b7280;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .expand-icon:hover {
    color: #374151;
  }
  
  .expand-placeholder {
    width: 1.25rem;
    display: inline-block;
  }
  
  .org-icon {
    font-size: 1.25rem;
  }
  
  .org-name {
    font-weight: 500;
    color: #111827;
  }
  
  .org-code {
    color: #6b7280;
    font-size: 0.875rem;
  }
  
  .status-badge {
    padding: 0.125rem 0.5rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 500;
    margin-left: auto;
  }
  
  @media (max-width: 640px) {
    .node-content {
      font-size: 0.875rem;
    }
    
    .org-code {
      display: none;
    }
  }
</style>
