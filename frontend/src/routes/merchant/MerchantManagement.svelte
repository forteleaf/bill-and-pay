<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '@/api/client';
  import { tenantStore } from '@/stores/tenant';
  import { format } from 'date-fns';
  import type { Merchant, Organization, MerchantOrgHistory, PagedResponse } from '@/types/api';
  import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  import { Separator } from '$lib/components/ui/separator';
  
  let merchants = $state<Merchant[]>([]);
  let organizations = $state<Organization[]>([]);
  let selectedMerchant = $state<Merchant | null>(null);
  let history = $state<MerchantOrgHistory[]>([]);
  
  let loading = $state(true);
  let error = $state<string | null>(null);
  let historyLoading = $state(false);
  
  let showMoveModal = $state(false);
  let targetOrgId = $state<string>('');
  let moveReason = $state<string>('');
  let moveLoading = $state(false);
  let moveError = $state<string | null>(null);
  let moveSuccess = $state<string | null>(null);
  
  async function loadMerchants() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    loading = true;
    error = null;
    
    try {
      const response = await apiClient.get<PagedResponse<Merchant>>('/merchants?page=0&size=100');
      
      if (response.success && response.data) {
        merchants = response.data.content;
      }
      
      loading = false;
    } catch (err) {
      error = 'Failed to load merchant data.';
      loading = false;
      console.error(err);
    }
  }
  
  async function loadOrganizations() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    
    try {
      const response = await apiClient.get<PagedResponse<Organization>>('/organizations?page=0&size=100');
      
      if (response.success && response.data) {
        organizations = response.data.content;
      }
    } catch (err) {
      console.error('Failed to load organizations:', err);
    }
  }
  
  async function loadHistory(merchantId: string) {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    historyLoading = true;
    
    try {
      const response = await apiClient.get<MerchantOrgHistory[]>(`/merchants/${merchantId}/history`);
      
      if (response.success && response.data) {
        history = response.data;
      }
      
      historyLoading = false;
    } catch (err) {
      console.error('Failed to load history:', err);
      history = [];
      historyLoading = false;
    }
  }
  
  function selectMerchant(merchant: Merchant) {
    selectedMerchant = merchant;
    loadHistory(merchant.id);
  }
  
  function openMoveModal() {
    if (!selectedMerchant) return;
    
    showMoveModal = true;
    targetOrgId = '';
    moveReason = '';
    moveError = null;
    moveSuccess = null;
  }
  
  function closeMoveModal() {
    showMoveModal = false;
    targetOrgId = '';
    moveReason = '';
    moveError = null;
    moveSuccess = null;
  }
  
  async function moveMerchant() {
    if (!selectedMerchant || !targetOrgId) {
      moveError = 'Please select a target organization.';
      return;
    }
    
    if (!moveReason.trim()) {
      moveError = 'Please enter a reason for the move.';
      return;
    }
    
    const merchantId = selectedMerchant.id;
    moveLoading = true;
    moveError = null;
    moveSuccess = null;
    
    try {
      const response = await apiClient.put<Merchant>(`/merchants/${merchantId}/move`, {
        targetOrgId: targetOrgId,
        reason: moveReason
      });
      
      if (response.success && response.data) {
        moveSuccess = 'Merchant organization move completed.';
        
        const index = merchants.findIndex(m => m.id === merchantId);
        if (index !== -1) {
          merchants[index] = response.data;
        }
        selectedMerchant = response.data;
        
        await loadHistory(response.data.id);
        
        setTimeout(() => {
          closeMoveModal();
        }, 1500);
      }
      
      moveLoading = false;
    } catch (err) {
      moveError = 'Failed to move organization.';
      moveLoading = false;
      console.error(err);
    }
  }
  
  function getOrgTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'DISTRIBUTOR': 'Distributor',
      'AGENCY': 'Agency',
      'DEALER': 'Dealer',
      'SELLER': 'Seller',
      'VENDOR': 'Vendor'
    };
    return labels[type] || type;
  }
  
  function getStatusVariant(status: string): 'default' | 'destructive' {
    return status === 'ACTIVE' ? 'default' : 'destructive';
  }
  
  function getStatusLabel(status: string): string {
    return status === 'ACTIVE' ? 'Active' : 'Inactive';
  }
  
  onMount(() => {
    loadMerchants();
    loadOrganizations();
  });
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div>
    <h1 class="text-3xl font-bold text-foreground">Merchant Management</h1>
    <p class="text-muted-foreground mt-1">Manage merchant organization moves and history</p>
  </div>
  
  {#if loading}
    <div class="text-center py-12 text-lg text-muted-foreground">Loading data...</div>
  {:else if error}
    <div class="text-center py-12 text-lg text-destructive">{error}</div>
  {:else}
    <div class="grid grid-cols-1 lg:grid-cols-[400px_1fr] gap-6">
      <Card class="h-fit max-h-[calc(100vh-200px)] flex flex-col">
        <CardHeader>
          <CardTitle>Merchant List</CardTitle>
        </CardHeader>
        <CardContent class="flex-1 overflow-y-auto space-y-2">
          {#each merchants as merchant}
            <button
              type="button"
              class="w-full p-4 border-2 rounded-lg text-left transition-colors flex justify-between items-center
                {selectedMerchant?.id === merchant.id 
                  ? 'border-primary bg-primary/5' 
                  : 'border-border hover:border-primary/50 hover:bg-muted/50'}"
              onclick={() => selectMerchant(merchant)}
            >
              <div class="flex-1 min-w-0">
                <div class="font-semibold text-foreground truncate">{merchant.name}</div>
                <div class="text-sm font-mono text-primary">{merchant.code}</div>
                <div class="text-xs font-mono text-muted-foreground truncate">{merchant.orgPath}</div>
              </div>
              <Badge variant={getStatusVariant(merchant.status)}>
                {getStatusLabel(merchant.status)}
              </Badge>
            </button>
          {/each}
          
          {#if merchants.length === 0}
            <div class="text-center py-8 text-muted-foreground">No merchants registered.</div>
          {/if}
        </CardContent>
      </Card>
      
      <Card>
        {#if selectedMerchant}
          <CardHeader class="flex flex-row items-center justify-between">
            <CardTitle>{selectedMerchant.name}</CardTitle>
            <Button onclick={openMoveModal}>
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Move Organization
            </Button>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="bg-muted/50 rounded-lg p-4 space-y-3">
              <div class="flex justify-between items-center py-2 border-b border-border">
                <span class="text-muted-foreground">Merchant Code</span>
                <span class="font-mono font-semibold text-primary">{selectedMerchant.code}</span>
              </div>
              <div class="flex justify-between items-center py-2 border-b border-border">
                <span class="text-muted-foreground">Current Org Path</span>
                <span class="font-mono text-sm">{selectedMerchant.orgPath}</span>
              </div>
              <div class="flex justify-between items-center py-2">
                <span class="text-muted-foreground">Status</span>
                <Badge variant={getStatusVariant(selectedMerchant.status)}>
                  {getStatusLabel(selectedMerchant.status)}
                </Badge>
              </div>
            </div>
            
            <Separator />
            
            <div>
              <h3 class="text-lg font-semibold mb-4">Move History</h3>
              {#if historyLoading}
                <div class="text-center py-8 text-muted-foreground">Loading history...</div>
              {:else if history.length > 0}
                <div class="relative pl-8 space-y-6">
                  {#each history as item, index}
                    <div class="relative">
                      <div class="absolute -left-8 top-1 w-3 h-3 bg-primary rounded-full border-2 border-background shadow"></div>
                      {#if index < history.length - 1}
                        <div class="absolute -left-[22px] top-4 w-0.5 h-full bg-border"></div>
                      {/if}
                      <div class="bg-muted/50 rounded-lg p-4 border-l-4 border-primary">
                        <div class="text-sm text-muted-foreground mb-2">
                          {format(new Date(item.movedAt), 'yyyy-MM-dd HH:mm:ss')}
                        </div>
                        <div class="flex items-center gap-2 font-mono text-sm mb-2">
                          <span class="text-destructive font-semibold">{item.fromOrgPath}</span>
                          <span class="text-muted-foreground">-></span>
                          <span class="text-green-600 font-semibold">{item.toOrgPath}</span>
                        </div>
                        {#if item.reason}
                          <div class="text-sm text-foreground italic">Reason: {item.reason}</div>
                        {/if}
                        <div class="text-sm text-muted-foreground">Moved by: {item.movedBy}</div>
                      </div>
                    </div>
                  {/each}
                </div>
              {:else}
                <div class="text-center py-8 text-muted-foreground bg-muted/50 rounded-lg">
                  No move history.
                </div>
              {/if}
            </div>
          </CardContent>
        {:else}
          <CardContent class="flex items-center justify-center min-h-[400px]">
            <p class="text-muted-foreground text-lg">Select a merchant to view details</p>
          </CardContent>
        {/if}
      </Card>
    </div>
  {/if}
</div>

{#if showMoveModal}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onclick={closeMoveModal}>
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div onclick={(e) => e.stopPropagation()}>
      <Card class="w-full max-w-lg">
        <CardHeader class="flex flex-row items-center justify-between">
          <CardTitle>Move Merchant Organization</CardTitle>
          <Button variant="ghost" size="sm" onclick={closeMoveModal}>
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </Button>
        </CardHeader>
        
        <CardContent class="space-y-4">
          {#if moveSuccess}
            <div class="p-4 rounded-lg bg-green-100 text-green-800 text-center font-semibold">
              {moveSuccess}
            </div>
          {:else}
            <div class="bg-muted/50 rounded-lg p-4">
              <div class="text-sm text-muted-foreground">Current Organization</div>
              <div class="font-mono font-semibold text-primary">{selectedMerchant?.orgPath}</div>
            </div>
            
            <div class="space-y-2">
              <Label for="targetOrg">Target Organization *</Label>
              <select 
                id="targetOrg" 
                bind:value={targetOrgId}
                class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="">Select an organization</option>
                {#each organizations as org}
                  <option value={org.id}>
                    {org.name} ({getOrgTypeLabel(org.orgType)}) - {org.path}
                  </option>
                {/each}
              </select>
            </div>
            
            <div class="space-y-2">
              <Label for="reason">Reason *</Label>
              <textarea 
                id="reason" 
                bind:value={moveReason}
                placeholder="Enter the reason for the move"
                rows={4}
                class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-none"
              ></textarea>
            </div>
            
            {#if moveError}
              <div class="p-3 rounded-md bg-destructive/10 border border-destructive/20 text-destructive text-sm">
                {moveError}
              </div>
            {/if}
          {/if}
        </CardContent>
        
        <CardFooter class="flex justify-end gap-3">
          <Button variant="outline" onclick={closeMoveModal} disabled={moveLoading}>
            Cancel
          </Button>
          <Button onclick={moveMerchant} disabled={moveLoading || !!moveSuccess}>
            {moveLoading ? 'Moving...' : 'Confirm'}
          </Button>
        </CardFooter>
      </Card>
    </div>
  </div>
{/if}
