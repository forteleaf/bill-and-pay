<script lang="ts">
  import type { Organization, OrgTree as OrgTreeType } from '../types/api';
  import { apiClient } from '../lib/api';
  import OrgTreeNode from './OrgTreeNode.svelte';
  
  interface Props {
    rootOrgId: string;
    selectedId?: string | null;
    onSelect: (org: Organization) => void;
    class?: string;
  }
  
  let {
    rootOrgId,
    selectedId = null,
    onSelect,
    class: className = ''
  }: Props = $props();
  
  let organizations = $state<OrgTreeType[]>([]);
  let loading = $state<boolean>(false);
  let error = $state<string | null>(null);
  
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
    
    return roots;
  }
  
  $effect(() => {
    const loadTree = async () => {
      loading = true;
      error = null;
      try {
        const response = await apiClient.get<Organization[]>(
          `/organizations/${rootOrgId}/descendants`
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
    };
    loadTree();
  });
</script>

<div class="org-tree {className}">
  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <span>Loading organizations...</span>
    </div>
  {:else if error}
    <div class="error">
      <span>⚠️ {error}</span>
    </div>
  {:else if organizations.length === 0}
    <div class="empty">
      <span>No organizations found</span>
    </div>
  {:else}
    {#each organizations as org (org.id)}
      <OrgTreeNode
        node={org}
        level={0}
        {selectedId}
        {onSelect}
      />
    {/each}
  {/if}
</div>

<style>
  .org-tree {
    border: 1px solid #e5e7eb;
    border-radius: 0.5rem;
    background: white;
    min-height: 400px;
    max-height: 600px;
    overflow-y: auto;
    padding: 1rem;
  }
  
  .loading,
  .error,
  .empty {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    color: #6b7280;
  }
  
  .loading {
    flex-direction: column;
    gap: 1rem;
  }
  
  .spinner {
    width: 2rem;
    height: 2rem;
    border: 3px solid #e5e7eb;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  
  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
  
  .error {
    color: #dc2626;
  }
  
  @media (max-width: 640px) {
    .org-tree {
      min-height: 300px;
      max-height: 500px;
      padding: 0.5rem;
    }
  }
</style>
