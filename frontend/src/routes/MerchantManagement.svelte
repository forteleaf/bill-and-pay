<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { Merchant, Organization, MerchantOrgHistory, PagedResponse } from '../types/api';
  
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
      error = '가맹점 데이터를 불러오는데 실패했습니다.';
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
      moveError = '대상 조직을 선택해주세요.';
      return;
    }
    
    if (!moveReason.trim()) {
      moveError = '이동 사유를 입력해주세요.';
      return;
    }
    
    moveLoading = true;
    moveError = null;
    moveSuccess = null;
    
    try {
      const response = await apiClient.put<Merchant>(`/merchants/${selectedMerchant.id}/move`, {
        targetOrgId: targetOrgId,
        reason: moveReason
      });
      
      if (response.success && response.data) {
        moveSuccess = '가맹점 조직 이동이 완료되었습니다.';
        
        // Update merchant in list
        const index = merchants.findIndex(m => m.id === selectedMerchant.id);
        if (index !== -1) {
          merchants[index] = response.data;
        }
        selectedMerchant = response.data;
        
        // Reload history
        await loadHistory(selectedMerchant.id);
        
        // Close modal after 1.5 seconds
        setTimeout(() => {
          closeMoveModal();
        }, 1500);
      }
      
      moveLoading = false;
    } catch (err) {
      moveError = '조직 이동에 실패했습니다.';
      moveLoading = false;
      console.error(err);
    }
  }
  
  function getOrgTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'DISTRIBUTOR': '총판',
      'AGENCY': '대리점',
      'DEALER': '딜러',
      'SELLER': '판매점',
      'VENDOR': '벤더'
    };
    return labels[type] || type;
  }
  
  function getStatusBadge(status: string): string {
    return status === 'ACTIVE' ? 'success' : 'danger';
  }
  
  function getStatusLabel(status: string): string {
    return status === 'ACTIVE' ? '활성' : '비활성';
  }
  
  $effect(() => {
    loadMerchants();
    loadOrganizations();
  });
</script>

<div class="merchant-management">
  <div class="header">
    <h1>가맹점 관리</h1>
    <p class="subtitle">가맹점 조직 이동 및 이력 관리</p>
  </div>
  
  {#if loading}
    <div class="loading">데이터를 불러오는 중...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else}
    <div class="content-grid">
      <div class="merchant-list">
        <h2>가맹점 목록</h2>
        <div class="list-container">
          {#each merchants as merchant}
            <div 
              class="merchant-item {selectedMerchant?.id === merchant.id ? 'selected' : ''}"
              onclick={() => selectMerchant(merchant)}
            >
              <div class="merchant-info">
                <div class="merchant-name">{merchant.name}</div>
                <div class="merchant-code">{merchant.code}</div>
                <div class="merchant-path">{merchant.orgPath}</div>
              </div>
              <span class="badge badge-{getStatusBadge(merchant.status)}">
                {getStatusLabel(merchant.status)}
              </span>
            </div>
          {/each}
          
          {#if merchants.length === 0}
            <div class="empty">등록된 가맹점이 없습니다.</div>
          {/if}
        </div>
      </div>
      
      <div class="merchant-details">
        {#if selectedMerchant}
          <div class="details-header">
            <h2>{selectedMerchant.name}</h2>
            <button class="btn-primary" onclick={openMoveModal}>
              🔄 조직 이동
            </button>
          </div>
          
          <div class="info-card">
            <div class="info-row">
              <span class="label">가맹점 코드</span>
              <span class="value code">{selectedMerchant.code}</span>
            </div>
            <div class="info-row">
              <span class="label">현재 조직 경로</span>
              <span class="value path">{selectedMerchant.orgPath}</span>
            </div>
            <div class="info-row">
              <span class="label">상태</span>
              <span class="badge badge-{getStatusBadge(selectedMerchant.status)}">
                {getStatusLabel(selectedMerchant.status)}
              </span>
            </div>
          </div>
          
          <h3>이동 이력</h3>
          {#if historyLoading}
            <div class="loading-small">이력을 불러오는 중...</div>
          {:else if history.length > 0}
            <div class="history-timeline">
              {#each history as item}
                <div class="timeline-item">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <div class="timeline-date">
                      {format(new Date(item.movedAt), 'yyyy-MM-dd HH:mm:ss')}
                    </div>
                    <div class="timeline-move">
                      <span class="from">{item.fromOrgPath}</span>
                      <span class="arrow">→</span>
                      <span class="to">{item.toOrgPath}</span>
                    </div>
                    {#if item.reason}
                      <div class="timeline-reason">
                        사유: {item.reason}
                      </div>
                    {/if}
                    <div class="timeline-user">
                      이동자: {item.movedBy}
                    </div>
                  </div>
                </div>
              {/each}
            </div>
          {:else}
            <div class="empty-history">이동 이력이 없습니다.</div>
          {/if}
        {:else}
          <div class="no-selection">
            ← 가맹점을 선택하면 상세 정보가 표시됩니다.
          </div>
        {/if}
      </div>
    </div>
  {/if}
</div>

{#if showMoveModal}
  <div class="modal-overlay" onclick={closeMoveModal}></div>
  <div class="modal">
    <div class="modal-header">
      <h2>가맹점 조직 이동</h2>
      <button class="modal-close" onclick={closeMoveModal}>×</button>
    </div>
    
    <div class="modal-body">
      {#if moveSuccess}
        <div class="success-message">{moveSuccess}</div>
      {:else}
        <div class="current-info">
          <div class="info-label">현재 조직</div>
          <div class="info-value">{selectedMerchant?.orgPath}</div>
        </div>
        
        <div class="form-group">
          <label for="targetOrg">이동할 조직 *</label>
          <select id="targetOrg" bind:value={targetOrgId}>
            <option value="">조직을 선택하세요</option>
            {#each organizations as org}
              <option value={org.id}>
                {org.name} ({getOrgTypeLabel(org.orgType)}) - {org.path}
              </option>
            {/each}
          </select>
        </div>
        
        <div class="form-group">
          <label for="reason">이동 사유 *</label>
          <textarea 
            id="reason" 
            bind:value={moveReason}
            placeholder="조직 이동 사유를 입력하세요"
            rows="4"
          ></textarea>
        </div>
        
        {#if moveError}
          <div class="error-message">{moveError}</div>
        {/if}
      {/if}
    </div>
    
    <div class="modal-footer">
      <button class="btn-secondary" onclick={closeMoveModal} disabled={moveLoading}>
        취소
      </button>
      <button 
        class="btn-primary" 
        onclick={moveMerchant} 
        disabled={moveLoading || !!moveSuccess}
      >
        {moveLoading ? '이동 중...' : '확인'}
      </button>
    </div>
  </div>
{/if}

<style>
  .merchant-management {
    max-width: 1400px;
    margin: 0 auto;
  }
  
  .header {
    margin-bottom: 2rem;
  }
  
  h1 {
    font-size: 2rem;
    font-weight: 700;
    margin-bottom: 0.5rem;
  }
  
  .subtitle {
    color: #666;
    font-size: 0.95rem;
  }
  
  .loading {
    text-align: center;
    padding: 3rem;
    font-size: 1.1rem;
  }
  
  .error {
    text-align: center;
    padding: 3rem;
    font-size: 1.1rem;
    color: #dc2626;
  }
  
  .content-grid {
    display: grid;
    grid-template-columns: 400px 1fr;
    gap: 2rem;
  }
  
  /* Merchant List */
  .merchant-list {
    background: white;
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    height: fit-content;
    max-height: calc(100vh - 200px);
    display: flex;
    flex-direction: column;
  }
  
  .merchant-list h2 {
    font-size: 1.25rem;
    font-weight: 700;
    margin-bottom: 1rem;
    color: #1f2937;
  }
  
  .list-container {
    overflow-y: auto;
    flex: 1;
  }
  
  .merchant-item {
    padding: 1rem;
    border: 2px solid #e5e7eb;
    border-radius: 0.5rem;
    margin-bottom: 0.75rem;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .merchant-item:hover {
    border-color: #667eea;
    background: #f9fafb;
  }
  
  .merchant-item.selected {
    border-color: #667eea;
    background: #ede9fe;
  }
  
  .merchant-info {
    flex: 1;
  }
  
  .merchant-name {
    font-size: 1rem;
    font-weight: 600;
    margin-bottom: 0.25rem;
    color: #1f2937;
  }
  
  .merchant-code {
    font-size: 0.875rem;
    font-family: monospace;
    color: #667eea;
    margin-bottom: 0.25rem;
  }
  
  .merchant-path {
    font-size: 0.75rem;
    font-family: monospace;
    color: #6b7280;
  }
  
  /* Merchant Details */
  .merchant-details {
    background: white;
    border-radius: 1rem;
    padding: 2rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  
  .details-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }
  
  .details-header h2 {
    font-size: 1.5rem;
    font-weight: 700;
    color: #1f2937;
  }
  
  .no-selection {
    text-align: center;
    padding: 3rem;
    color: #9ca3af;
    font-size: 1.1rem;
  }
  
  .info-card {
    background: #f9fafb;
    border-radius: 0.75rem;
    padding: 1.5rem;
    margin-bottom: 2rem;
  }
  
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 0;
    border-bottom: 1px solid #e5e7eb;
  }
  
  .info-row:last-child {
    border-bottom: none;
  }
  
  .info-row .label {
    font-weight: 500;
    color: #6b7280;
    font-size: 0.9rem;
  }
  
  .info-row .value {
    font-weight: 600;
    color: #1f2937;
  }
  
  .info-row .value.code {
    font-family: monospace;
    color: #667eea;
  }
  
  .info-row .value.path {
    font-family: monospace;
    font-size: 0.9rem;
  }
  
  h3 {
    font-size: 1.125rem;
    font-weight: 700;
    margin-bottom: 1rem;
    color: #1f2937;
  }
  
  .loading-small {
    text-align: center;
    padding: 2rem;
    color: #6b7280;
  }
  
  .empty-history {
    text-align: center;
    padding: 2rem;
    color: #9ca3af;
    background: #f9fafb;
    border-radius: 0.5rem;
  }
  
  /* Timeline */
  .history-timeline {
    position: relative;
    padding-left: 2rem;
  }
  
  .timeline-item {
    position: relative;
    padding-bottom: 2rem;
  }
  
  .timeline-item:last-child {
    padding-bottom: 0;
  }
  
  .timeline-dot {
    position: absolute;
    left: -2rem;
    top: 0.5rem;
    width: 12px;
    height: 12px;
    background: #667eea;
    border-radius: 50%;
    border: 3px solid white;
    box-shadow: 0 0 0 2px #667eea;
  }
  
  .timeline-item:not(:last-child)::before {
    content: '';
    position: absolute;
    left: calc(-2rem + 4px);
    top: 1.25rem;
    width: 2px;
    height: calc(100% - 0.5rem);
    background: #e5e7eb;
  }
  
  .timeline-content {
    background: #f9fafb;
    padding: 1rem;
    border-radius: 0.5rem;
    border-left: 3px solid #667eea;
  }
  
  .timeline-date {
    font-size: 0.875rem;
    color: #6b7280;
    margin-bottom: 0.5rem;
  }
  
  .timeline-move {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
    font-family: monospace;
    font-size: 0.9rem;
  }
  
  .timeline-move .from {
    color: #dc2626;
    font-weight: 600;
  }
  
  .timeline-move .arrow {
    color: #6b7280;
  }
  
  .timeline-move .to {
    color: #10b981;
    font-weight: 600;
  }
  
  .timeline-reason {
    font-size: 0.875rem;
    color: #374151;
    margin-bottom: 0.25rem;
    font-style: italic;
  }
  
  .timeline-user {
    font-size: 0.875rem;
    color: #6b7280;
  }
  
  /* Badges */
  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
  }
  
  .badge-success {
    background: #d1fae5;
    color: #065f46;
  }
  
  .badge-danger {
    background: #fee2e2;
    color: #991b1b;
  }
  
  /* Buttons */
  .btn-primary {
    padding: 0.5rem 1.5rem;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
  }
  
  .btn-primary:hover:not(:disabled) {
    background: #5568d3;
  }
  
  .btn-primary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  .btn-secondary {
    padding: 0.5rem 1.5rem;
    background: white;
    color: #374151;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .btn-secondary:hover:not(:disabled) {
    background: #f9fafb;
    border-color: #9ca3af;
  }
  
  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  .empty {
    text-align: center;
    padding: 2rem;
    color: #9ca3af;
  }
  
  /* Modal */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
    animation: fadeIn 0.2s;
  }
  
  .modal {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: white;
    border-radius: 1rem;
    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
    z-index: 1001;
    width: 90%;
    max-width: 600px;
    animation: slideIn 0.3s;
  }
  
  @keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }
  
  @keyframes slideIn {
    from {
      opacity: 0;
      transform: translate(-50%, -45%);
    }
    to {
      opacity: 1;
      transform: translate(-50%, -50%);
    }
  }
  
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e5e7eb;
  }
  
  .modal-header h2 {
    font-size: 1.25rem;
    font-weight: 700;
    color: #1f2937;
  }
  
  .modal-close {
    background: none;
    border: none;
    font-size: 2rem;
    color: #9ca3af;
    cursor: pointer;
    padding: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.25rem;
    transition: all 0.2s;
  }
  
  .modal-close:hover {
    background: #f9fafb;
    color: #374151;
  }
  
  .modal-body {
    padding: 1.5rem;
  }
  
  .current-info {
    background: #f9fafb;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
  }
  
  .info-label {
    font-size: 0.875rem;
    color: #6b7280;
    margin-bottom: 0.25rem;
  }
  
  .info-value {
    font-family: monospace;
    font-weight: 600;
    color: #667eea;
  }
  
  .form-group {
    margin-bottom: 1.5rem;
  }
  
  .form-group label {
    display: block;
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: #374151;
  }
  
  .form-group select,
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    font-size: 0.95rem;
    font-family: inherit;
  }
  
  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }
  
  .form-group textarea {
    resize: vertical;
  }
  
  .error-message {
    padding: 0.75rem;
    background: #fee2e2;
    color: #991b1b;
    border-radius: 0.5rem;
    font-size: 0.9rem;
    margin-top: 1rem;
  }
  
  .success-message {
    padding: 1rem;
    background: #d1fae5;
    color: #065f46;
    border-radius: 0.5rem;
    text-align: center;
    font-weight: 600;
  }
  
  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    padding: 1.5rem;
    border-top: 1px solid #e5e7eb;
  }
  
  @media (max-width: 1024px) {
    .content-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
