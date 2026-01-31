<script lang="ts">
  import { branchApi } from '../../lib/branchApi';
  import {
    BRANCH_TYPE_LABELS,
    OrgType,
    type Branch,
    type BranchUpdateRequest,
    type BusinessInfo
  } from '../../types/branch';

  interface Props {
    branchId: string;
  }

  let { branchId }: Props = $props();

  let branch = $state<Branch | null>(null);
  let loading = $state(true);
  let editMode = $state(false);
  let saving = $state(false);
  let error = $state<string | null>(null);

  // Editable fields
  let editName = $state('');
  let editPhone = $state('');
  let editEmail = $state('');
  let editManagerName = $state('');
  let editManagerPhone = $state('');
  let editAddress = $state('');

  // Active section tab
  let activeSection = $state<'basic' | 'fee' | 'settlement'>('basic');

  // Fee tab
  let activeFeeTab = $state<'terminal' | 'oldAuth' | 'nonAuth' | 'authPay' | 'recurring'>('terminal');

  const feeTabs: Array<{ key: 'terminal' | 'oldAuth' | 'nonAuth' | 'authPay' | 'recurring'; label: string }> = [
    { key: 'terminal', label: '단말기' },
    { key: 'oldAuth', label: '구인증' },
    { key: 'nonAuth', label: '비인증' },
    { key: 'authPay', label: '인증결제' },
    { key: 'recurring', label: '정기과금' }
  ];

  $effect(() => {
    if (branchId) {
      loadBranch();
    }
  });

  async function loadBranch() {
    loading = true;
    error = null;

    try {
      const response = await branchApi.getBranchById(branchId);
      if (response.success && response.data) {
        branch = response.data;
        initEditFields();
      } else {
        error = response.error?.message || '정보를 불러올 수 없습니다.';
      }
    } catch (err) {
      error = '정보를 불러올 수 없습니다.';
    } finally {
      loading = false;
    }
  }

  function initEditFields() {
    if (!branch) return;
    editName = branch.name || '';
    editPhone = branch.businessInfo?.mainPhone || '';
    editEmail = branch.businessInfo?.email || '';
    editManagerName = branch.businessInfo?.managerName || '';
    editManagerPhone = branch.businessInfo?.managerPhone || '';
    editAddress = branch.businessInfo?.businessAddress || '';
  }

  function toggleEditMode() {
    editMode = !editMode;
    if (!editMode && branch) {
      initEditFields();
    }
  }

  async function handleSave() {
    if (!branch) return;

    saving = true;
    error = null;

    try {
      const businessInfo: BusinessInfo = {
        businessType: branch.businessInfo?.businessType || 'CORPORATION' as BusinessInfo['businessType'],
        representative: branch.businessInfo?.representative || '',
        businessAddress: editAddress,
        mainPhone: editPhone,
        managerName: editManagerName,
        managerPhone: editManagerPhone,
        email: editEmail,
        businessNumber: branch.businessInfo?.businessNumber,
        corporateNumber: branch.businessInfo?.corporateNumber,
        openDate: branch.businessInfo?.openDate,
        actualAddress: branch.businessInfo?.actualAddress,
        businessCategory: branch.businessInfo?.businessCategory,
        businessType2: branch.businessInfo?.businessType2
      };

      const data: BranchUpdateRequest = {
        name: editName,
        phone: editPhone,
        email: editEmail,
        businessInfo
      };

      const response = await branchApi.updateBranch(branch.id, data);

      if (response.success && response.data) {
        branch = response.data;
        editMode = false;
        alert('저장되었습니다.');
      } else {
        error = response.error?.message || '저장에 실패했습니다.';
      }
    } catch (err) {
      error = '저장에 실패했습니다.';
    } finally {
      saving = false;
    }
  }

  function formatStatus(status: string): string {
    const statusMap: Record<string, string> = {
      'ACTIVE': '활성',
      'SUSPENDED': '정지',
      'TERMINATED': '해지'
    };
    return statusMap[status] || status;
  }

  function getStatusClass(status: string): string {
    return `status-${status?.toLowerCase() || 'default'}`;
  }
</script>

<div class="branch-detail">
  {#if loading}
    <div class="loading-state">
      <div class="spinner"></div>
      <span>불러오는 중...</span>
    </div>
  {:else if error}
    <div class="error-state">
      <span class="error-icon">!</span>
      <span>{error}</span>
      <button class="btn btn-secondary" onclick={loadBranch}>다시 시도</button>
    </div>
  {:else if branch}
    <!-- Header -->
    <div class="detail-header">
      <div class="header-info">
        <h1 class="branch-name">{branch.name}</h1>
        <div class="header-meta">
          <span class="branch-code">{branch.orgCode}</span>
          <span class="branch-type">{BRANCH_TYPE_LABELS[branch.orgType as OrgType] || branch.orgType}</span>
          <span class={`status-badge ${getStatusClass(branch.status)}`}>{formatStatus(branch.status)}</span>
        </div>
      </div>
      <div class="header-actions">
        {#if editMode}
          <button class="btn btn-secondary" onclick={toggleEditMode} disabled={saving}>취소</button>
          <button class="btn btn-primary" onclick={handleSave} disabled={saving}>
            {saving ? '저장 중...' : '저장'}
          </button>
        {:else}
          <button class="btn btn-primary" onclick={toggleEditMode}>수정</button>
        {/if}
      </div>
    </div>

    <!-- Section Tabs -->
    <div class="section-tabs">
      <button
        class="section-tab"
        class:active={activeSection === 'basic'}
        onclick={() => activeSection = 'basic'}
      >
        기본정보
      </button>
      <button
        class="section-tab"
        class:active={activeSection === 'fee'}
        onclick={() => activeSection = 'fee'}
      >
        수수료설정
      </button>
      <button
        class="section-tab"
        class:active={activeSection === 'settlement'}
        onclick={() => activeSection = 'settlement'}
      >
        정산내역
      </button>
    </div>

    <!-- Content -->
    <div class="detail-content">
      {#if activeSection === 'basic'}
        <!-- 기본정보 섹션 -->
        <div class="content-card">
          <h3 class="card-title">사업자정보</h3>
          <div class="info-grid">
            <div class="info-item">
              <label>사업자번호</label>
              <span class="value mono">{branch.businessInfo?.businessNumber || '-'}</span>
            </div>
            <div class="info-item">
              <label>상호</label>
              {#if editMode}
                <input type="text" bind:value={editName} class="edit-input" />
              {:else}
                <span class="value">{branch.name}</span>
              {/if}
            </div>
            <div class="info-item">
              <label>대표자</label>
              <span class="value">{branch.businessInfo?.representative || '-'}</span>
            </div>
            <div class="info-item">
              <label>연락처</label>
              {#if editMode}
                <input type="text" bind:value={editPhone} class="edit-input" placeholder="010-0000-0000" />
              {:else}
                <span class="value">{branch.businessInfo?.mainPhone || '-'}</span>
              {/if}
            </div>
            <div class="info-item full-width">
              <label>주소</label>
              {#if editMode}
                <input type="text" bind:value={editAddress} class="edit-input" />
              {:else}
                <span class="value">{branch.businessInfo?.businessAddress || '-'}</span>
              {/if}
            </div>
            <div class="info-item">
              <label>담당자</label>
              {#if editMode}
                <input type="text" bind:value={editManagerName} class="edit-input" />
              {:else}
                <span class="value">{branch.businessInfo?.managerName || '-'}</span>
              {/if}
            </div>
            <div class="info-item">
              <label>담당자 연락처</label>
              {#if editMode}
                <input type="text" bind:value={editManagerPhone} class="edit-input" placeholder="010-0000-0000" />
              {:else}
                <span class="value">{branch.businessInfo?.managerPhone || '-'}</span>
              {/if}
            </div>
            <div class="info-item full-width">
              <label>이메일</label>
              {#if editMode}
                <input type="email" bind:value={editEmail} class="edit-input" placeholder="example@email.com" />
              {:else}
                <span class="value">{branch.businessInfo?.email || '-'}</span>
              {/if}
            </div>
          </div>
        </div>

        <div class="content-card">
          <h3 class="card-title">정산계좌</h3>
          <div class="info-grid">
            <div class="info-item">
              <label>은행</label>
              <span class="value">{branch.bankAccount?.bankName || '-'}</span>
            </div>
            <div class="info-item">
              <label>계좌번호</label>
              <span class="value mono">{branch.bankAccount?.accountNumber || '-'}</span>
            </div>
            <div class="info-item">
              <label>예금주</label>
              <span class="value">{branch.bankAccount?.accountHolder || '-'}</span>
            </div>
          </div>
        </div>

        <div class="content-card">
          <h3 class="card-title">한도설정</h3>
          <div class="info-grid">
            <div class="info-item">
              <label>1회 한도</label>
              <span class="value highlight">{(branch.limitConfig?.perTransaction || 0).toLocaleString()}백만원</span>
            </div>
            <div class="info-item">
              <label>1일 한도</label>
              <span class="value highlight">{(branch.limitConfig?.perDay || 0).toLocaleString()}백만원</span>
            </div>
          </div>
        </div>

      {:else if activeSection === 'fee'}
        <!-- 수수료설정 섹션 -->
        <div class="content-card">
          <div class="fee-tabs">
            {#each feeTabs as tab}
              <button
                class="fee-tab"
                class:active={activeFeeTab === tab.key}
                onclick={() => activeFeeTab = tab.key}
              >
                {tab.label}
              </button>
            {/each}
          </div>
          
          <div class="fee-content">
            {#if branch.feeConfig?.[activeFeeTab]}
              <div class="fee-grid">
                <div class="fee-item">
                  <label>일반</label>
                  <span class="fee-value">{branch.feeConfig[activeFeeTab]!.general}%</span>
                </div>
                <div class="fee-item">
                  <label>영세</label>
                  <span class="fee-value">{branch.feeConfig[activeFeeTab]!.small}%</span>
                </div>
                <div class="fee-item">
                  <label>중소1</label>
                  <span class="fee-value">{branch.feeConfig[activeFeeTab]!.medium1}%</span>
                </div>
                <div class="fee-item">
                  <label>중소2</label>
                  <span class="fee-value">{branch.feeConfig[activeFeeTab]!.medium2}%</span>
                </div>
                <div class="fee-item">
                  <label>중소3</label>
                  <span class="fee-value">{branch.feeConfig[activeFeeTab]!.medium3}%</span>
                </div>
                <div class="fee-item">
                  <label>해외카드</label>
                  <span class="fee-value">{branch.feeConfig[activeFeeTab]!.foreign}%</span>
                </div>
              </div>
            {:else}
              <div class="no-data">설정된 수수료가 없습니다.</div>
            {/if}
          </div>
        </div>

      {:else if activeSection === 'settlement'}
        <!-- 정산내역 섹션 -->
        <div class="content-card">
          <div class="coming-soon">
            <span class="coming-soon-icon">📊</span>
            <h3>정산내역</h3>
            <p>정산내역 조회 기능이 곧 제공될 예정입니다.</p>
          </div>
        </div>
      {/if}
    </div>
  {/if}
</div>

<style>
  .branch-detail {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #f5f5f5;
  }

  /* Loading & Error States */
  .loading-state,
  .error-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 1rem;
    color: #5f6368;
    padding: 3rem;
  }

  .spinner {
    width: 36px;
    height: 36px;
    border: 3px solid #e8eaed;
    border-top-color: #6366f1;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .error-state {
    color: #d93025;
  }

  .error-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: #fce8e6;
    color: #d93025;
    font-size: 1.5rem;
    font-weight: 700;
  }

  /* Header */
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 1.5rem;
    background: white;
    border-bottom: 1px solid #e5e7eb;
  }

  .header-info {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .branch-name {
    font-size: 1.5rem;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0;
  }

  .header-meta {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .branch-code {
    font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
    font-size: 0.8125rem;
    color: #6b7280;
  }

  .branch-type {
    font-size: 0.8125rem;
    color: #6366f1;
    font-weight: 500;
  }

  .header-actions {
    display: flex;
    gap: 0.5rem;
  }

  /* Buttons */
  .btn {
    padding: 0.5rem 1rem;
    font-size: 0.875rem;
    font-weight: 500;
    border-radius: 6px;
    border: none;
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-primary {
    background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
  }

  .btn-primary:hover:not(:disabled) {
    background: linear-gradient(135deg, #4338ca 0%, #4f46e5 100%);
    transform: translateY(-1px);
  }

  .btn-secondary {
    background: #f1f3f4;
    color: #5f6368;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #e8eaed;
  }

  /* Status Badge */
  .status-badge {
    display: inline-flex;
    align-items: center;
    padding: 0.25rem 0.625rem;
    font-size: 0.6875rem;
    font-weight: 600;
    border-radius: 100px;
    text-transform: uppercase;
  }

  .status-active {
    background: #dcfce7;
    color: #166534;
  }

  .status-suspended {
    background: #fef3c7;
    color: #b45309;
  }

  .status-terminated {
    background: #fee2e2;
    color: #dc2626;
  }

  /* Section Tabs */
  .section-tabs {
    display: flex;
    gap: 0;
    background: white;
    border-bottom: 1px solid #e5e7eb;
    padding: 0 1.5rem;
  }

  .section-tab {
    padding: 1rem 1.5rem;
    font-size: 0.875rem;
    font-weight: 500;
    color: #6b7280;
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .section-tab:hover {
    color: #374151;
  }

  .section-tab.active {
    color: #6366f1;
    border-bottom-color: #6366f1;
  }

  /* Content */
  .detail-content {
    flex: 1;
    overflow-y: auto;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .content-card {
    background: white;
    border-radius: 12px;
    padding: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  }

  .card-title {
    font-size: 1rem;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0 0 1.25rem 0;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid #e5e7eb;
  }

  /* Info Grid */
  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1.25rem;
  }

  .info-item {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }

  .info-item.full-width {
    grid-column: 1 / -1;
  }

  .info-item label {
    font-size: 0.75rem;
    font-weight: 500;
    color: #6b7280;
  }

  .info-item .value {
    font-size: 0.9375rem;
    color: #1f2937;
  }

  .info-item .value.mono {
    font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
    font-size: 0.875rem;
    color: #4b5563;
  }

  .info-item .value.highlight {
    font-weight: 600;
    color: #6366f1;
  }

  /* Edit Input */
  .edit-input {
    padding: 0.5rem 0.75rem;
    font-size: 0.9375rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    background: white;
    color: #1f2937;
    transition: all 0.15s ease;
  }

  .edit-input:focus {
    outline: none;
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
  }

  /* Fee Tabs */
  .fee-tabs {
    display: flex;
    gap: 0.25rem;
    padding: 0.25rem;
    background: #f3f4f6;
    border-radius: 8px;
    margin-bottom: 1.25rem;
  }

  .fee-tab {
    flex: 1;
    padding: 0.625rem;
    font-size: 0.8125rem;
    font-weight: 500;
    color: #6b7280;
    background: transparent;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .fee-tab:hover {
    color: #374151;
  }

  .fee-tab.active {
    background: white;
    color: #1f2937;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }

  /* Fee Content */
  .fee-content {
    background: #f9fafb;
    border-radius: 8px;
    padding: 1.25rem;
  }

  .fee-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1rem;
  }

  .fee-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.375rem;
    padding: 1rem;
    background: white;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
  }

  .fee-item label {
    font-size: 0.75rem;
    font-weight: 500;
    color: #6b7280;
  }

  .fee-value {
    font-size: 1.25rem;
    font-weight: 600;
    color: #6366f1;
  }

  .no-data {
    text-align: center;
    color: #9ca3af;
    font-size: 0.875rem;
    padding: 2rem;
  }

  /* Coming Soon */
  .coming-soon {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem;
    text-align: center;
    color: #6b7280;
  }

  .coming-soon-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
  }

  .coming-soon h3 {
    font-size: 1.125rem;
    font-weight: 600;
    color: #374151;
    margin: 0 0 0.5rem 0;
  }

  .coming-soon p {
    font-size: 0.875rem;
    color: #9ca3af;
    margin: 0;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .info-grid {
      grid-template-columns: 1fr;
    }

    .fee-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .detail-header {
      flex-direction: column;
      gap: 1rem;
    }

    .header-actions {
      width: 100%;
    }

    .header-actions .btn {
      flex: 1;
    }
  }
</style>
