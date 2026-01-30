<script lang="ts">
  import type { Organization, OrgType } from '../types/api';
  import { apiClient } from '../lib/api';
  
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
    const indent = '└─ '.repeat(Math.max(0, level - 1));
    return `${indent}${org.name} (${org.orgCode}) [${org.orgType}]`;
  }
  
  $effect(() => {
    const loadParentOptions = async () => {
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
    };
    loadParentOptions();
  });
</script>

<div class="parent-selector">
  <label for="parent-select" class="label">
    Parent Organization
    {#if nextOrgType}
      <span class="hint">(Will create {nextOrgType})</span>
    {/if}
  </label>
  
  {#if loading}
    <div class="loading">Loading...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else}
    <select
      id="parent-select"
      bind:value={selectedParentId}
      onchange={handleSelect}
      {disabled}
      class="select"
    >
      <option value="">Select parent...</option>
      {#each parentOptions as org (org.id)}
        <option value={org.id}>
          {getIndentedLabel(org)}
        </option>
      {/each}
    </select>
  {/if}
  
  {#if !nextOrgType && selectedParentId}
    <p class="warning">
      ⚠️ Selected organization cannot have child organizations
    </p>
  {/if}
</div>

<style>
  .parent-selector {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .label {
    font-weight: 500;
    color: #374151;
  }
  
  .hint {
    color: #6b7280;
    font-weight: 400;
    font-size: 0.875rem;
  }
  
  .select {
    padding: 0.5rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    color: #111827;
    background-color: white;
    cursor: pointer;
  }
  
  .select:disabled {
    background-color: #f3f4f6;
    cursor: not-allowed;
    opacity: 0.6;
  }
  
  .select:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  .loading,
  .error {
    padding: 0.5rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.875rem;
  }
  
  .loading {
    background-color: #f3f4f6;
    color: #6b7280;
  }
  
  .error {
    background-color: #fee2e2;
    color: #dc2626;
  }
  
  .warning {
    padding: 0.5rem 0.75rem;
    background-color: #fef3c7;
    color: #d97706;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    margin: 0;
  }
  
  @media (max-width: 640px) {
    .select {
      font-size: 0.8125rem;
    }
  }
</style>
