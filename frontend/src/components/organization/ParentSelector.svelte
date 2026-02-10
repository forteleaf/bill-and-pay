<script lang="ts">
  import { onMount } from 'svelte';
  import type { Organization, OrgType } from '@/types/api';
  import { apiClient } from '@/api/client';
  import { Label } from '$lib/components/ui/label';
  import * as Select from '$lib/components/ui/select';
  
  interface Props {
    selectedParentId?: string | null;
    currentUserOrgId: string;
    currentUserOrgType: OrgType;
    onSelect: (parentId: string, allowedChildType: OrgType) => void;
    disabled?: boolean;
  }
  
  let {
    selectedParentId = $bindable(null),
    currentUserOrgId,
    currentUserOrgType,
    onSelect,
    disabled = false
  }: Props = $props();
  
  let parentOptions = $state<Organization[]>([]);
  let loading = $state<boolean>(false);
  let error = $state<string | null>(null);
  
  const orgTypeHierarchy: Record<OrgType, OrgType | null> = {
    DISTRIBUTOR: 'AGENCY' as OrgType,
    AGENCY: 'DEALER' as OrgType,
    DEALER: 'SELLER' as OrgType,
    SELLER: 'VENDOR' as OrgType,
    VENDOR: null
  };
  
  const allowedChildType = $derived(
    selectedParentId
      ? parentOptions.find(p => p.id === selectedParentId)?.orgType as OrgType
      : currentUserOrgType
  );
  
  const nextOrgType = $derived(
    allowedChildType ? orgTypeHierarchy[allowedChildType] : null
  );
  
  function handleSelect(e: Event) {
    const target = e.target as HTMLSelectElement;
    const parentId = target.value;
    if (parentId && nextOrgType) {
      onSelect(parentId, nextOrgType);
    }
  }
  
  function getIndentedLabel(org: Organization): string {
    const level = org.level || 0;
    const indent = '-- '.repeat(Math.max(0, level - 1));
    return `${indent}${org.name} (${org.orgCode}) [${org.orgType}]`;
  }
  
  async function loadParentOptions() {
    loading = true;
    error = null;
    try {
      const response = await apiClient.get<Organization[]>(
        `/organizations/${currentUserOrgId}/descendants`
      );
      if (response.success && response.data) {
        const allOrgs = response.data;
        
        const currentUserOrg = allOrgs.find(o => o.id === currentUserOrgId);
        if (currentUserOrg) {
          allOrgs.unshift(currentUserOrg);
        }
        
        parentOptions = allOrgs.filter(org => {
          const childType = orgTypeHierarchy[org.orgType as OrgType];
          return childType !== null;
        });
      } else {
        error = response.error?.message || 'Failed to load parent options';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load parent options';
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    loadParentOptions();
  });
</script>

<div class="flex flex-col gap-2">
  <Label for="parent-select" class="flex items-center gap-2">
    Parent Organization
    {#if nextOrgType}
      <span class="text-muted-foreground font-normal text-sm">(Will create {nextOrgType})</span>
    {/if}
  </Label>
  
  {#if loading}
    <div class="p-2 rounded-md bg-muted text-muted-foreground text-sm">Loading...</div>
  {:else if error}
    <div class="p-2 rounded-md bg-destructive/10 text-destructive text-sm">{error}</div>
  {:else}
    <Select.Root type="single"
      value={selectedParentId ?? ""}
      onValueChange={(v) => {
        selectedParentId = v || null;
        if (v) {
          const event = { target: { value: v } } as any;
          handleSelect(event);
        }
      }}
      disabled={disabled}>
      <Select.Trigger class="w-full">
        {#if selectedParentId}
          {parentOptions.find(o => o.id === selectedParentId)?.name || selectedParentId}
        {:else}
          <span class="text-muted-foreground">상위 조직 선택</span>
        {/if}
      </Select.Trigger>
      <Select.Content>
        {#each parentOptions as org (org.id)}
          <Select.Item value={org.id}>{getIndentedLabel(org)}</Select.Item>
        {/each}
      </Select.Content>
    </Select.Root>
  {/if}
  
  {#if !nextOrgType && selectedParentId}
    <p class="p-2 bg-yellow-50 text-yellow-700 rounded-md text-sm m-0">
      Warning: Selected organization cannot have child organizations
    </p>
  {/if}
</div>
