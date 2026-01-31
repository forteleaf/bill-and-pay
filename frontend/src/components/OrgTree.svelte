<script lang="ts">
  import { onMount } from 'svelte';
  import type { Organization, OrgTree as OrgTreeType } from '../types/api';
  import { apiClient } from '../lib/api';
  import OrgTreeNode from './OrgTreeNode.svelte';
  import { Card } from '$lib/components/ui/card';
  
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
  
  async function loadTree() {
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
  }

  onMount(() => {
    loadTree();
  });
</script>

<Card class="min-h-[400px] max-h-[600px] overflow-y-auto p-4 {className}">
  {#if loading}
    <div class="flex flex-col items-center justify-center min-h-[400px] gap-4 text-muted-foreground">
      <div class="w-8 h-8 border-3 border-border border-t-primary rounded-full animate-spin"></div>
      <span>Loading organizations...</span>
    </div>
  {:else if error}
    <div class="flex items-center justify-center min-h-[400px] text-destructive">
      <span>Warning: {error}</span>
    </div>
  {:else if organizations.length === 0}
    <div class="flex items-center justify-center min-h-[400px] text-muted-foreground">
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
</Card>
