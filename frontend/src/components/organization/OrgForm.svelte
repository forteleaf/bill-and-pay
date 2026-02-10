<script lang="ts">
  import type { Organization, OrgType, OrgStatus, CreateOrgRequest, UpdateOrgRequest } from '@/types/api';
  import ParentSelector from './ParentSelector.svelte';
  import ConfirmModal from '@/components/shared/ConfirmModal.svelte';
  import { apiClient } from '@/api/client';
  import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import * as Select from '$lib/components/ui/select';
  
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
  
  // Use $derived for reactive access to initialData
  const initialName = $derived(initialData?.name || '');
  const initialOrgType = $derived((initialData?.orgType || '') as OrgType);
  const initialStatus = $derived((initialData?.status || 'ACTIVE') as OrgStatus);
  const initialBusinessEntityId = $derived(initialData?.businessEntityId || '');
  
  let formData = $state({
    name: '',
    orgType: '' as OrgType,
    parentId: '',
    status: 'ACTIVE' as OrgStatus,
    businessEntityId: '',
    email: '',
    phone: '',
    address: ''
  });
  
  // Sync formData with initialData when it changes
  $effect(() => {
    formData.name = initialName;
    formData.orgType = initialOrgType;
    formData.status = initialStatus;
    formData.businessEntityId = initialBusinessEntityId;
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
    (mode === 'create' ? formData.parentId && formData.orgType && formData.businessEntityId : true)
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
          businessEntityId: formData.businessEntityId,
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

<Card>
  <form onsubmit={handleSubmit}>
    <CardHeader>
      <CardTitle>{mode === 'create' ? 'Create Organization' : 'Edit Organization'}</CardTitle>
    </CardHeader>
    
    <CardContent class="space-y-4">
      {#if submitError}
        <div class="p-3 rounded-md bg-destructive/10 border border-destructive/20 text-destructive text-sm">
          Warning: {submitError}
        </div>
      {/if}
      
      {#if mode === 'create'}
        <ParentSelector
          bind:selectedParentId={formData.parentId}
          {currentUserOrgId}
          {currentUserOrgType}
          onSelect={handleParentSelect}
        />
      {/if}
      
      {#if mode === 'create' && formData.orgType}
        <div class="space-y-2">
          <Label>Organization Type</Label>
          <Input
            type="text"
            value={formData.orgType}
            disabled
          />
        </div>
      {/if}
      
      {#if mode === 'create'}
        <div class="space-y-2">
          <Label for="businessEntityId">Business Entity ID *</Label>
          <Input
            id="businessEntityId"
            type="text"
            value={formData.businessEntityId}
            oninput={(e) => formData.businessEntityId = e.currentTarget.value}
            placeholder="Enter business entity ID"
            required
          />
          <span class="text-xs text-muted-foreground">Associated business entity for this organization</span>
        </div>
      {/if}
      
      {#if mode === 'edit' && initialData}
        <div class="space-y-2">
          <Label>Organization Code</Label>
          <Input
            type="text"
            value={initialData.orgCode}
            disabled
          />
          <span class="text-xs text-muted-foreground">Auto-generated, cannot be changed</span>
        </div>
      {/if}
      
      <div class="space-y-2">
        <Label for="name">Name *</Label>
        <Input
          id="name"
          type="text"
          value={formData.name}
          oninput={(e) => formData.name = e.currentTarget.value}
          onblur={() => touched.name = true}
          class={errors.name ? 'border-destructive' : ''}
          required
        />
        {#if errors.name}
          <span class="text-xs text-destructive">{errors.name}</span>
        {/if}
      </div>
      
      {#if mode === 'edit'}
        <div class="space-y-2">
          <Label for="status">Status</Label>
          <Select.Root type="single"
            value={formData.status}
            onValueChange={(v) => handleStatusChange(v as OrgStatus)}>
            <Select.Trigger class="w-full">
              {#if formData.status}
                {formData.status === 'ACTIVE' ? 'Active' : formData.status === 'SUSPENDED' ? 'Suspended' : 'Terminated'}
              {:else}
                <span class="text-muted-foreground">상태 선택</span>
              {/if}
            </Select.Trigger>
            <Select.Content>
              <Select.Item value="ACTIVE">Active</Select.Item>
              <Select.Item value="SUSPENDED">Suspended</Select.Item>
              <Select.Item value="TERMINATED">Terminated</Select.Item>
            </Select.Content>
          </Select.Root>
        </div>
      {/if}
      
      <div class="space-y-2">
        <Label for="email">Email</Label>
        <Input
          id="email"
          type="email"
          value={formData.email}
          oninput={(e) => formData.email = e.currentTarget.value}
          onblur={() => touched.email = true}
          class={errors.email ? 'border-destructive' : ''}
        />
        {#if errors.email}
          <span class="text-xs text-destructive">{errors.email}</span>
        {/if}
      </div>
      
      <div class="space-y-2">
        <Label for="phone">Phone</Label>
        <Input
          id="phone"
          type="tel"
          value={formData.phone}
          oninput={(e) => formData.phone = e.currentTarget.value}
          onblur={() => touched.phone = true}
          placeholder="010-XXXX-XXXX"
          class={errors.phone ? 'border-destructive' : ''}
        />
        {#if errors.phone}
          <span class="text-xs text-destructive">{errors.phone}</span>
        {/if}
      </div>
      
      <div class="space-y-2">
        <Label for="address">Address</Label>
        <textarea
          id="address"
          bind:value={formData.address}
          rows={3}
          class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-none"
        ></textarea>
      </div>
    </CardContent>
    
    <CardFooter class="flex justify-end gap-3 border-t pt-6">
      <Button
        type="button"
        variant="outline"
        onclick={onCancel}
        disabled={submitting}
      >
        Cancel
      </Button>
      <Button
        type="submit"
        disabled={!isValid || submitting}
      >
        {submitting ? 'Saving...' : mode === 'create' ? 'Create' : 'Update'}
      </Button>
    </CardFooter>
  </form>
</Card>

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
