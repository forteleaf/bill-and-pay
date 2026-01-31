<script lang="ts">
  import type { Organization } from '../types/api';
  import { OrgType } from '../types/api';
  import OrgTree from '../components/OrgTree.svelte';
  import OrgForm from '../components/OrgForm.svelte';
  
  let selectedOrg = $state<Organization | null>(null);
  let mode = $state<'view' | 'create' | 'edit'>('view');
  let treeKey = $state(0);
  
  const currentUserOrgId = 'temp-root-org-id';
  const currentUserOrgType = OrgType.DISTRIBUTOR;
  
  function handleTreeSelect(org: Organization) {
    if (mode === 'view') {
      selectedOrg = org;
    }
  }
  
  function handleCreate() {
    mode = 'create';
    selectedOrg = null;
  }
  
  function handleEdit() {
    if (selectedOrg) {
      mode = 'edit';
    }
  }
  
  function handleCancel() {
    mode = 'view';
  }
  
  function handleFormSubmit(data: Organization) {
    mode = 'view';
    selectedOrg = data;
    treeKey += 1;
  }
</script>

<div class="organizations-page">
  <div class="page-header">
    <h1>🏢 Organization Management</h1>
    {#if mode === 'view'}
      <button class="btn btn-primary" onclick={handleCreate}>
        + Create Organization
      </button>
    {/if}
  </div>
  
  <div class="content">
    <div class="tree-panel">
      {#key treeKey}
        <OrgTree
          rootOrgId={currentUserOrgId}
          selectedId={selectedOrg?.id}
          onSelect={handleTreeSelect}
        />
      {/key}
    </div>
    
    <div class="detail-panel">
      {#if mode === 'create' || mode === 'edit'}
        <OrgForm
          mode={mode}
          initialData={mode === 'edit' ? selectedOrg : null}
          {currentUserOrgId}
          {currentUserOrgType}
          onSubmit={handleFormSubmit}
          onCancel={handleCancel}
        />
      {:else if selectedOrg}
        <div class="detail-view">
          <div class="detail-header">
            <h2>{selectedOrg.name}</h2>
            <button class="btn btn-secondary" onclick={handleEdit}>
              ✏️ Edit
            </button>
          </div>
          
          <div class="detail-grid">
            <div class="detail-field">
              <label>Organization Code</label>
              <div class="detail-value">{selectedOrg.orgCode}</div>
            </div>
            
            <div class="detail-field">
              <label>Type</label>
              <div class="detail-value">{selectedOrg.orgType}</div>
            </div>
            
            <div class="detail-field">
              <label>Status</label>
              <div class="detail-value">
                <span class="status-badge status-{selectedOrg.status.toLowerCase()}">
                  {selectedOrg.status}
                </span>
              </div>
            </div>
            
            <div class="detail-field">
              <label>Path</label>
              <div class="detail-value mono">{selectedOrg.path}</div>
            </div>
            
            <div class="detail-field">
              <label>Level</label>
              <div class="detail-value">{selectedOrg.level}</div>
            </div>
          </div>
        </div>
      {:else}
        <div class="empty-state">
          <p>Select an organization from the tree to view details</p>
          <p class="hint">or</p>
          <button class="btn btn-primary" onclick={handleCreate}>
            + Create New Organization
          </button>
        </div>
      {/if}
    </div>
  </div>
</div>

<style>
  .organizations-page {
    padding: 2rem;
    max-width: 1600px;
    margin: 0 auto;
  }
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }
  
  .page-header h1 {
    font-size: 1.875rem;
    font-weight: 700;
    color: #111827;
    margin: 0;
  }
  
  .content {
    display: grid;
    grid-template-columns: 30% 70%;
    gap: 1.5rem;
    min-height: 600px;
  }
  
  .tree-panel {
    display: flex;
    flex-direction: column;
  }
  
  .detail-panel {
    display: flex;
    flex-direction: column;
  }
  
  .detail-view {
    background: white;
    border-radius: 0.5rem;
    border: 1px solid #e5e7eb;
    padding: 1.5rem;
  }
  
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid #e5e7eb;
  }
  
  .detail-header h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #111827;
    margin: 0;
  }
  
  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1.5rem;
  }
  
  .detail-field {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .detail-field label {
    font-size: 0.875rem;
    font-weight: 500;
    color: #6b7280;
  }
  
  .detail-value {
    font-size: 1rem;
    color: #111827;
  }
  
  .detail-value.mono {
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 0.875rem;
    background: #f3f4f6;
    padding: 0.5rem;
    border-radius: 0.25rem;
  }
  
  .status-badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.875rem;
    font-weight: 500;
  }
  
  .status-active {
    background-color: #d1fae5;
    color: #065f46;
  }
  
  .status-suspended {
    background-color: #fef3c7;
    color: #92400e;
  }
  
  .status-terminated {
    background-color: #fee2e2;
    color: #991b1b;
  }
  
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    gap: 1rem;
    background: white;
    border-radius: 0.5rem;
    border: 1px solid #e5e7eb;
    padding: 2rem;
  }
  
  .empty-state p {
    color: #6b7280;
    margin: 0;
  }
  
  .empty-state .hint {
    font-size: 0.875rem;
    color: #9ca3af;
  }
  
  .btn {
    padding: 0.5rem 1.5rem;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: none;
  }
  
  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  .btn-primary {
    background-color: #3b82f6;
    color: white;
  }
  
  .btn-primary:hover:not(:disabled) {
    background-color: #2563eb;
  }
  
  .btn-secondary {
    background-color: #f3f4f6;
    color: #374151;
  }
  
  .btn-secondary:hover:not(:disabled) {
    background-color: #e5e7eb;
  }
  
  @media (max-width: 1024px) {
    .content {
      grid-template-columns: 1fr;
    }
    
    .tree-panel {
      max-height: 400px;
    }
    
    .detail-grid {
      grid-template-columns: 1fr;
    }
  }
  
  @media (max-width: 640px) {
    .organizations-page {
      padding: 1rem;
    }
    
    .page-header {
      flex-direction: column;
      gap: 1rem;
      align-items: stretch;
    }
    
    .page-header h1 {
      font-size: 1.5rem;
    }
    
    .btn {
      width: 100%;
    }
  }
</style>
