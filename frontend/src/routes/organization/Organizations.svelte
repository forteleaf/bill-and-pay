<script lang="ts">
  import type { Organization } from '@/types/api';
  import { OrgType } from '@/types/api';
  import OrgTree from '@/components/organization/OrgTree.svelte';
  import OrgForm from '@/components/organization/OrgForm.svelte';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { formatBusinessNumber } from '@/utils/formatters';
  
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

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' {
    if (status === 'ACTIVE') return 'default';
    if (status === 'SUSPENDED') return 'secondary';
    return 'destructive';
  }
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div class="flex justify-between items-center">
    <div>
      <h1 class="text-3xl font-bold text-foreground flex items-center gap-2">
        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
        Organization Management
      </h1>
    </div>
    {#if mode === 'view'}
      <Button onclick={handleCreate}>
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Create Organization
      </Button>
    {/if}
  </div>
  
  <div class="grid grid-cols-1 lg:grid-cols-[30%_70%] gap-6 min-h-[600px]">
    <div class="flex flex-col">
      {#key treeKey}
        <OrgTree
          rootOrgId={currentUserOrgId}
          selectedId={selectedOrg?.id}
          onSelect={handleTreeSelect}
        />
      {/key}
    </div>
    
    <div class="flex flex-col">
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
        <Card>
          <CardHeader class="flex flex-row items-center justify-between border-b">
            <CardTitle>{selectedOrg.name}</CardTitle>
            <Button variant="secondary" onclick={handleEdit}>
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              Edit
            </Button>
          </CardHeader>
          <CardContent class="pt-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-1">
                <div class="text-sm font-medium text-muted-foreground">Organization Code</div>
                <div class="text-foreground">{selectedOrg.orgCode}</div>
              </div>
              
              <div class="space-y-1">
                <div class="text-sm font-medium text-muted-foreground">Type</div>
                <div class="text-foreground">{selectedOrg.orgType}</div>
              </div>
              
              <div class="space-y-1">
                <div class="text-sm font-medium text-muted-foreground">Status</div>
                <Badge variant={getStatusVariant(selectedOrg.status)}>
                  {selectedOrg.status}
                </Badge>
              </div>
              
              <div class="space-y-1">
                <div class="text-sm font-medium text-muted-foreground">Path</div>
                <div class="font-mono text-sm bg-muted p-2 rounded">{selectedOrg.path}</div>
              </div>
              
              <div class="space-y-1">
                <div class="text-sm font-medium text-muted-foreground">Level</div>
                <div class="text-foreground">{selectedOrg.level}</div>
              </div>

              {#if selectedOrg.businessEntity}
                <div class="space-y-1">
                  <div class="text-sm font-medium text-muted-foreground">사업자번호</div>
                  <div class="text-foreground">
                    {selectedOrg.businessEntity.businessType === 'NON_BUSINESS'
                      ? '비사업자'
                      : formatBusinessNumber(selectedOrg.businessEntity.businessNumber)}
                  </div>
                </div>

                <div class="space-y-1">
                  <div class="text-sm font-medium text-muted-foreground">사업자유형</div>
                  <div class="text-foreground">{selectedOrg.businessEntity.businessType}</div>
                </div>
              {/if}
            </div>
          </CardContent>
        </Card>
      {:else}
        <Card class="flex-1">
          <CardContent class="flex flex-col items-center justify-center min-h-[400px] gap-4">
            <p class="text-muted-foreground">Select an organization from the tree to view details</p>
            <p class="text-sm text-muted-foreground/70">or</p>
            <Button onclick={handleCreate}>
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Create New Organization
            </Button>
          </CardContent>
        </Card>
      {/if}
    </div>
  </div>
</div>
