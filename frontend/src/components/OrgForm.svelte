<script lang="ts">
  import type { Organization, OrgType, OrgStatus, CreateOrgRequest, UpdateOrgRequest } from '../types/api';
  import ParentSelector from './ParentSelector.svelte';
  import ConfirmModal from './ConfirmModal.svelte';
  import { apiClient } from '../lib/api';
  
  interface Props {
    mode: 'create' | 'edit';
    initialData?: Organization | null;
    currentUserOrgId: string;
    currentUserOrgType: OrgType;
    onSubmit: (data: Organization) => void;
    onCancel: () => void;
  }
  
  let {
    mode,
    initialData = null,
    currentUserOrgId,
    currentUserOrgType,
    onSubmit,
    onCancel
  }: Props = $props();
  
  let formData = $state({
    name: initialData?.name || '',
    orgType: (initialData?.orgType || '') as OrgType,
    parentId: '',
    status: (initialData?.status || 'ACTIVE') as OrgStatus,
    email: '',
    phone: '',
    address: ''
  });
  
  let touched = $state({
    name: false,
    email: false,
    phone: false
  });
  
  let submitting = $state(false);
  let submitError = $state<string | null>(null);
  let showStatusChangeModal = $state(false);
  let pendingStatus = $state<OrgStatus | null>(null);
  
  const errors = $derived({
    name: touched.name && formData.name.length < 3 ? 'Name must be at least 3 characters' : null,
    email: touched.email && formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email) 
      ? 'Invalid email format' : null,
    phone: touched.phone && formData.phone && !/^010-?\d{4}-?\d{4}$/.test(formData.phone)
      ? 'Invalid phone format (010-XXXX-XXXX)' : null
  });
  
  const isValid = $derived(
    formData.name.length >= 3 &&
    !errors.email &&
    !errors.phone &&
    (mode === 'create' ? formData.parentId && formData.orgType : true)
  );
  
  function handleParentSelect(parentId: string, allowedChildType: OrgType) {
    formData.parentId = parentId;
    formData.orgType = allowedChildType;
  }
  
  function handleStatusChange(newStatus: OrgStatus) {
    if (mode === 'edit' && newStatus !== initialData?.status) {
      pendingStatus = newStatus;
      showStatusChangeModal = true;
    } else {
      formData.status = newStatus;
    }
  }
  
  function confirmStatusChange() {
    if (pendingStatus) {
      formData.status = pendingStatus;
    }
    showStatusChangeModal = false;
    pendingStatus = null;
  }
  
  function cancelStatusChange() {
    formData.status = (initialData?.status || 'ACTIVE') as OrgStatus;
    showStatusChangeModal = false;
    pendingStatus = null;
  }
  
  async function handleSubmit(e: Event) {
    e.preventDefault();
    
    touched.name = true;
    touched.email = true;
    touched.phone = true;
    
    if (!isValid || submitting) {
      return;
    }
    
    submitting = true;
    submitError = null;
    
    try {
      if (mode === 'create') {
        const createData: CreateOrgRequest = {
          name: formData.name,
          orgType: formData.orgType,
          parentId: formData.parentId || undefined,
          email: formData.email || undefined,
          phone: formData.phone || undefined,
          address: formData.address || undefined
        };
        
        const response = await apiClient.post<Organization>('/organizations', createData);
        if (response.success && response.data) {
          onSubmit(response.data);
        } else {
          submitError = response.error?.message || 'Failed to create organization';
        }
      } else if (initialData) {
        const updateData: UpdateOrgRequest = {
          name: formData.name !== initialData.name ? formData.name : undefined,
          status: formData.status !== initialData.status ? formData.status : undefined,
          email: formData.email || undefined,
          phone: formData.phone || undefined,
          address: formData.address || undefined
        };
        
        const response = await apiClient.put<Organization>(`/organizations/${initialData.id}`, updateData);
        if (response.success && response.data) {
          onSubmit(response.data);
        } else {
          submitError = response.error?.message || 'Failed to update organization';
        }
      }
    } catch (error) {
      submitError = error instanceof Error ? error.message : 'An error occurred';
    } finally {
      submitting = false;
    }
  }
</script>

<form class="org-form" onsubmit={handleSubmit}>
  <div class="form-header">
    <h2>{mode === 'create' ? 'Create Organization' : 'Edit Organization'}</h2>
  </div>
  
  {#if submitError}
    <div class="alert alert-error">
      ⚠️ {submitError}
    </div>
  {/if}
  
  <div class="form-fields">
    {#if mode === 'create'}
      <ParentSelector
        bind:selectedParentId={formData.parentId}
        {currentUserOrgId}
        {currentUserOrgType}
        onSelect={handleParentSelect}
      />
    {/if}
    
    {#if mode === 'create' && formData.orgType}
      <div class="form-field">
        <label class="label">Organization Type</label>
        <input
          type="text"
          value={formData.orgType}
          disabled
          class="input"
        />
      </div>
    {/if}
    
    {#if mode === 'edit' && initialData}
      <div class="form-field">
        <label class="label">Organization Code</label>
        <input
          type="text"
          value={initialData.orgCode}
          disabled
          class="input"
        />
        <span class="hint">Auto-generated, cannot be changed</span>
      </div>
    {/if}
    
    <div class="form-field">
      <label for="name" class="label">Name *</label>
      <input
        id="name"
        type="text"
        bind:value={formData.name}
        onblur={() => touched.name = true}
        class="input"
        class:error={errors.name}
        required
      />
      {#if errors.name}
        <span class="error-message">{errors.name}</span>
      {/if}
    </div>
    
    {#if mode === 'edit'}
      <div class="form-field">
        <label for="status" class="label">Status</label>
        <select
          id="status"
          value={formData.status}
          onchange={(e) => handleStatusChange(e.currentTarget.value as OrgStatus)}
          class="input"
        >
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="TERMINATED">Terminated</option>
        </select>
      </div>
    {/if}
    
    <div class="form-field">
      <label for="email" class="label">Email</label>
      <input
        id="email"
        type="email"
        bind:value={formData.email}
        onblur={() => touched.email = true}
        class="input"
        class:error={errors.email}
      />
      {#if errors.email}
        <span class="error-message">{errors.email}</span>
      {/if}
    </div>
    
    <div class="form-field">
      <label for="phone" class="label">Phone</label>
      <input
        id="phone"
        type="tel"
        bind:value={formData.phone}
        onblur={() => touched.phone = true}
        placeholder="010-XXXX-XXXX"
        class="input"
        class:error={errors.phone}
      />
      {#if errors.phone}
        <span class="error-message">{errors.phone}</span>
      {/if}
    </div>
    
    <div class="form-field">
      <label for="address" class="label">Address</label>
      <textarea
        id="address"
        bind:value={formData.address}
        rows="3"
        class="input"
      ></textarea>
    </div>
  </div>
  
  <div class="form-actions">
    <button
      type="button"
      onclick={onCancel}
      class="btn btn-secondary"
      disabled={submitting}
    >
      Cancel
    </button>
    <button
      type="submit"
      class="btn btn-primary"
      disabled={!isValid || submitting}
    >
      {submitting ? 'Saving...' : mode === 'create' ? 'Create' : 'Update'}
    </button>
  </div>
</form>

<ConfirmModal
  bind:show={showStatusChangeModal}
  title="Confirm Status Change"
  message="Are you sure you want to change the organization status? This action will affect all related operations."
  type="warning"
  confirmText="Confirm"
  cancelText="Cancel"
  onConfirm={confirmStatusChange}
  onCancel={cancelStatusChange}
/>

<style>
  .org-form {
    background: white;
    border-radius: 0.5rem;
    border: 1px solid #e5e7eb;
    padding: 1.5rem;
  }
  
  .form-header {
    margin-bottom: 1.5rem;
  }
  
  .form-header h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #111827;
    margin: 0;
  }
  
  .alert {
    padding: 0.75rem 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
  }
  
  .alert-error {
    background-color: #fee2e2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }
  
  .form-fields {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  
  .form-field {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .label {
    font-weight: 500;
    color: #374151;
    font-size: 0.875rem;
  }
  
  .hint {
    font-size: 0.75rem;
    color: #6b7280;
  }
  
  .input {
    padding: 0.5rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    color: #111827;
    background-color: white;
  }
  
  .input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  .input:disabled {
    background-color: #f3f4f6;
    cursor: not-allowed;
    opacity: 0.6;
  }
  
  .input.error {
    border-color: #dc2626;
  }
  
  .error-message {
    font-size: 0.75rem;
    color: #dc2626;
  }
  
  textarea.input {
    resize: vertical;
    font-family: inherit;
  }
  
  .form-actions {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e5e7eb;
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
  
  @media (max-width: 640px) {
    .org-form {
      padding: 1rem;
    }
    
    .form-actions {
      flex-direction: column;
    }
    
    .btn {
      width: 100%;
    }
  }
</style>
