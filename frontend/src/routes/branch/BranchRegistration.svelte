<script lang="ts">
  import { branchApi } from '../../lib/branchApi';
  import {
    OrgType,
    BusinessType,
    BRANCH_TYPE_LABELS,
    type BranchCreateRequest,
    type BusinessInfo,
    type BankAccountInfo,
    type FeeConfig,
    type LimitConfig
  } from '../../types/branch';

  // Step management
  let currentStep = $state(1);
  const totalSteps = 3;

  // Step 1: Basic Info
  let orgType = $state<OrgType>(OrgType.DISTRIBUTOR);
  let orgName = $state('');
  let businessInfo = $state<BusinessInfo>({
    businessType: BusinessType.CORPORATION,
    businessNumber: '',
    corporateNumber: '',
    representative: '',
    openDate: '',
    businessAddress: '',
    actualAddress: '',
    businessCategory: '',
    businessType2: '',
    mainPhone: '',
    managerName: '',
    managerPhone: '',
    email: ''
  });
  let bankAccount = $state<BankAccountInfo>({
    bankCode: '',
    bankName: '',
    accountNumber: '',
    accountHolder: ''
  });

  // Step 2: Fee Config
  const createDefaultFeeConfig = (): FeeConfig => ({
    general: 0,
    small: 0,
    medium1: 0,
    medium2: 0,
    medium3: 0,
    foreign: 0
  });

  let feeConfig = $state<{
    terminal: FeeConfig;
    oldAuth: FeeConfig;
    nonAuth: FeeConfig;
    authPay: FeeConfig;
    recurring: FeeConfig;
  }>({
    terminal: createDefaultFeeConfig(),
    oldAuth: createDefaultFeeConfig(),
    nonAuth: createDefaultFeeConfig(),
    authPay: createDefaultFeeConfig(),
    recurring: createDefaultFeeConfig()
  });

  let limitConfig = $state<LimitConfig>({
    perTransaction: 0,
    perDay: 0
  });

  // Form state
  let loading = $state(false);
  let errors = $state<Record<string, string>>({});
  let submitResult = $state<{ success: boolean; message: string } | null>(null);

  // Bank list for dropdown
  const banks = [
    { code: '004', name: 'KB국민은행' },
    { code: '011', name: 'NH농협은행' },
    { code: '020', name: '우리은행' },
    { code: '088', name: '신한은행' },
    { code: '081', name: '하나은행' },
    { code: '003', name: 'IBK기업은행' },
    { code: '023', name: 'SC제일은행' },
    { code: '027', name: '씨티은행' },
    { code: '039', name: '경남은행' },
    { code: '034', name: '광주은행' },
    { code: '031', name: '대구은행' },
    { code: '032', name: '부산은행' },
    { code: '037', name: '전북은행' },
    { code: '035', name: '제주은행' },
    { code: '090', name: '카카오뱅크' },
    { code: '092', name: '토스뱅크' },
    { code: '089', name: '케이뱅크' }
  ];

  // Fee type labels
  const feeTypeLabels: Record<string, string> = {
    terminal: '단말기',
    oldAuth: '구인증',
    nonAuth: '비인증',
    authPay: '인증결제',
    recurring: '정기과금'
  };

  // Fee category labels
  const feeCategoryLabels: Record<keyof FeeConfig, string> = {
    general: '일반',
    small: '영세',
    medium1: '중소1',
    medium2: '중소2',
    medium3: '중소3',
    foreign: '해외'
  };

  // Business type labels
  const businessTypeLabels: Record<BusinessType, string> = {
    [BusinessType.CORPORATION]: '법인사업자',
    [BusinessType.INDIVIDUAL]: '개인사업자',
    [BusinessType.NON_BUSINESS]: '비사업자'
  };

  // Validation
  function validateStep1(): boolean {
    const newErrors: Record<string, string> = {};

    if (!orgName.trim()) {
      newErrors.orgName = '영업점명을 입력해주세요.';
    }
    if (!businessInfo.representative.trim()) {
      newErrors.representative = '대표자명을 입력해주세요.';
    }
    if (!businessInfo.businessAddress.trim()) {
      newErrors.businessAddress = '사업장 소재지를 입력해주세요.';
    }
    if (businessInfo.businessType !== BusinessType.NON_BUSINESS && !businessInfo.businessNumber?.trim()) {
      newErrors.businessNumber = '사업자등록번호를 입력해주세요.';
    }
    if (!bankAccount.bankCode) {
      newErrors.bankCode = '은행을 선택해주세요.';
    }
    if (!bankAccount.accountNumber.trim()) {
      newErrors.accountNumber = '계좌번호를 입력해주세요.';
    }
    if (!bankAccount.accountHolder.trim()) {
      newErrors.accountHolder = '예금주를 입력해주세요.';
    }

    errors = newErrors;
    return Object.keys(newErrors).length === 0;
  }

  function validateStep2(): boolean {
    const newErrors: Record<string, string> = {};

    if (limitConfig.perTransaction <= 0) {
      newErrors.perTransaction = '1회 한도를 입력해주세요.';
    }
    if (limitConfig.perDay <= 0) {
      newErrors.perDay = '1일 한도를 입력해주세요.';
    }
    if (limitConfig.perTransaction > limitConfig.perDay) {
      newErrors.perTransaction = '1회 한도는 1일 한도를 초과할 수 없습니다.';
    }

    errors = newErrors;
    return Object.keys(newErrors).length === 0;
  }

  function handleNext() {
    if (currentStep === 1 && validateStep1()) {
      currentStep = 2;
      errors = {};
    } else if (currentStep === 2 && validateStep2()) {
      currentStep = 3;
      errors = {};
    }
  }

  function handlePrev() {
    if (currentStep > 1) {
      currentStep--;
      errors = {};
    }
  }

  async function handleSubmit() {
    loading = true;
    submitResult = null;

    const requestData: BranchCreateRequest = {
      name: orgName,
      orgType: orgType,
      businessInfo: businessInfo,
      bankAccount: bankAccount,
      feeConfig: feeConfig,
      limitConfig: limitConfig
    };

    try {
      const response = await branchApi.createBranch(requestData);
      if (response.success && response.data) {
        submitResult = {
          success: true,
          message: `영업점 "${response.data.name}"이(가) 성공적으로 등록되었습니다.`
        };
      } else {
        submitResult = {
          success: false,
          message: response.error?.message || '등록에 실패했습니다. 다시 시도해주세요.'
        };
      }
    } catch (err) {
      submitResult = {
        success: false,
        message: err instanceof Error ? err.message : '네트워크 오류가 발생했습니다.'
      };
    } finally {
      loading = false;
    }
  }

  function handleBankSelect(code: string) {
    const selected = banks.find(b => b.code === code);
    if (selected) {
      bankAccount.bankCode = selected.code;
      bankAccount.bankName = selected.name;
    }
  }

  function resetForm() {
    currentStep = 1;
    orgType = OrgType.DISTRIBUTOR;
    orgName = '';
    businessInfo = {
      businessType: BusinessType.CORPORATION,
      businessNumber: '',
      corporateNumber: '',
      representative: '',
      openDate: '',
      businessAddress: '',
      actualAddress: '',
      businessCategory: '',
      businessType2: '',
      mainPhone: '',
      managerName: '',
      managerPhone: '',
      email: ''
    };
    bankAccount = {
      bankCode: '',
      bankName: '',
      accountNumber: '',
      accountHolder: ''
    };
    feeConfig = {
      terminal: createDefaultFeeConfig(),
      oldAuth: createDefaultFeeConfig(),
      nonAuth: createDefaultFeeConfig(),
      authPay: createDefaultFeeConfig(),
      recurring: createDefaultFeeConfig()
    };
    limitConfig = { perTransaction: 0, perDay: 0 };
    errors = {};
    submitResult = null;
  }

</script>

<div class="registration-page">
  <div class="page-header">
    <div class="header-content">
      <h1>영업점 등록</h1>
      <p class="subtitle">새로운 영업점을 등록합니다</p>
    </div>
  </div>

  <!-- Step Indicator -->
  <div class="step-indicator">
    <div class="step-line">
      <div class="step-progress" style="width: {((currentStep - 1) / (totalSteps - 1)) * 100}%"></div>
    </div>
    <div class="steps">
      {#each [1, 2, 3] as step}
        <div class="step" class:active={currentStep === step} class:completed={currentStep > step}>
          <div class="step-circle">
            {#if currentStep > step}
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            {:else}
              {step}
            {/if}
          </div>
          <span class="step-label">
            {#if step === 1}기본정보{:else if step === 2}수수료설정{:else}확인/등록{/if}
          </span>
        </div>
      {/each}
    </div>
  </div>

  <!-- Form Container -->
  <div class="form-container">
    {#if submitResult}
      <!-- Result Screen -->
      <div class="result-screen">
        <div class="result-icon" class:success={submitResult.success} class:error={!submitResult.success}>
          {#if submitResult.success}
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="16 10 10 16 8 14"/>
            </svg>
          {:else}
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="15" y1="9" x2="9" y2="15"/>
              <line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          {/if}
        </div>
        <h2>{submitResult.success ? '등록 완료' : '등록 실패'}</h2>
        <p>{submitResult.message}</p>
        <div class="result-actions">
          {#if submitResult.success}
            <button class="btn btn-secondary" onclick={resetForm}>새 영업점 등록</button>
            <a href="/branch" class="btn btn-primary">영업점 목록으로</a>
          {:else}
            <button class="btn btn-primary" onclick={() => submitResult = null}>다시 시도</button>
          {/if}
        </div>
      </div>
    {:else}
      <!-- Step 1: Basic Info -->
      {#if currentStep === 1}
        <div class="form-step">
          <div class="form-section">
            <h3 class="section-title">
              <span class="section-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
              기본 정보
            </h3>
            <div class="form-grid">
              <div class="form-group">
                <label for="orgType">영업점 유형 <span class="required">*</span></label>
                <select id="orgType" bind:value={orgType} class="form-select">
                  {#each Object.values(OrgType) as type}
                    <option value={type}>{BRANCH_TYPE_LABELS[type]}</option>
                  {/each}
                </select>
              </div>
              <div class="form-group">
                <label for="orgName">영업점명 <span class="required">*</span></label>
                <input
                  id="orgName"
                  type="text"
                  bind:value={orgName}
                  placeholder="영업점명을 입력하세요"
                  class="form-input"
                  class:error={errors.orgName}
                />
                {#if errors.orgName}<span class="error-msg">{errors.orgName}</span>{/if}
              </div>
            </div>
          </div>

          <div class="form-section">
            <h3 class="section-title">
              <span class="section-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                  <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                </svg>
              </span>
              사업자 정보
            </h3>
            <div class="form-grid cols-3">
              <div class="form-group">
                <label for="businessType">사업자 구분 <span class="required">*</span></label>
                <select id="businessType" bind:value={businessInfo.businessType} class="form-select">
                  {#each Object.values(BusinessType) as type}
                    <option value={type}>{businessTypeLabels[type]}</option>
                  {/each}
                </select>
              </div>
              {#if businessInfo.businessType !== BusinessType.NON_BUSINESS}
                <div class="form-group">
                  <label for="businessNumber">사업자등록번호 <span class="required">*</span></label>
                  <input
                    id="businessNumber"
                    type="text"
                    bind:value={businessInfo.businessNumber}
                    placeholder="000-00-00000"
                    class="form-input"
                    class:error={errors.businessNumber}
                  />
                  {#if errors.businessNumber}<span class="error-msg">{errors.businessNumber}</span>{/if}
                </div>
              {/if}
              {#if businessInfo.businessType === BusinessType.CORPORATION}
                <div class="form-group">
                  <label for="corporateNumber">법인등록번호</label>
                  <input
                    id="corporateNumber"
                    type="text"
                    bind:value={businessInfo.corporateNumber}
                    placeholder="000000-0000000"
                    class="form-input"
                  />
                </div>
              {/if}
            </div>
            <div class="form-grid">
              <div class="form-group">
                <label for="representative">대표자 <span class="required">*</span></label>
                <input
                  id="representative"
                  type="text"
                  bind:value={businessInfo.representative}
                  placeholder="대표자명"
                  class="form-input"
                  class:error={errors.representative}
                />
                {#if errors.representative}<span class="error-msg">{errors.representative}</span>{/if}
              </div>
              <div class="form-group">
                <label for="openDate">개업연월일</label>
                <input
                  id="openDate"
                  type="date"
                  bind:value={businessInfo.openDate}
                  class="form-input"
                />
              </div>
            </div>
            <div class="form-grid">
              <div class="form-group full-width">
                <label for="businessAddress">사업장 소재지 <span class="required">*</span></label>
                <input
                  id="businessAddress"
                  type="text"
                  bind:value={businessInfo.businessAddress}
                  placeholder="사업장 주소를 입력하세요"
                  class="form-input"
                  class:error={errors.businessAddress}
                />
                {#if errors.businessAddress}<span class="error-msg">{errors.businessAddress}</span>{/if}
              </div>
            </div>
            <div class="form-grid">
              <div class="form-group full-width">
                <label for="actualAddress">실사업장 소재지</label>
                <input
                  id="actualAddress"
                  type="text"
                  bind:value={businessInfo.actualAddress}
                  placeholder="실사업장 주소 (다를 경우)"
                  class="form-input"
                />
              </div>
            </div>
            <div class="form-grid cols-3">
              <div class="form-group">
                <label for="businessCategory">업태</label>
                <input
                  id="businessCategory"
                  type="text"
                  bind:value={businessInfo.businessCategory}
                  placeholder="예: 서비스업"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label for="businessType2">업종</label>
                <input
                  id="businessType2"
                  type="text"
                  bind:value={businessInfo.businessType2}
                  placeholder="예: 소프트웨어 개발"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label for="mainPhone">대표번호</label>
                <input
                  id="mainPhone"
                  type="tel"
                  bind:value={businessInfo.mainPhone}
                  placeholder="02-0000-0000"
                  class="form-input"
                />
              </div>
            </div>
            <div class="form-grid cols-3">
              <div class="form-group">
                <label for="managerName">담당자</label>
                <input
                  id="managerName"
                  type="text"
                  bind:value={businessInfo.managerName}
                  placeholder="담당자명"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label for="managerPhone">담당자 연락처</label>
                <input
                  id="managerPhone"
                  type="tel"
                  bind:value={businessInfo.managerPhone}
                  placeholder="010-0000-0000"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label for="email">이메일</label>
                <input
                  id="email"
                  type="email"
                  bind:value={businessInfo.email}
                  placeholder="example@email.com"
                  class="form-input"
                />
              </div>
            </div>
          </div>

          <div class="form-section">
            <h3 class="section-title">
              <span class="section-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="1" y="4" width="22" height="16" rx="2" ry="2"/>
                  <line x1="1" y1="10" x2="23" y2="10"/>
                </svg>
              </span>
              정산 계좌 정보
            </h3>
            <div class="form-grid">
              <div class="form-group">
                <label for="bankCode">은행 <span class="required">*</span></label>
                <select
                  id="bankCode"
                  value={bankAccount.bankCode}
                  onchange={(e) => handleBankSelect((e.target as HTMLSelectElement).value)}
                  class="form-select"
                  class:error={errors.bankCode}
                >
                  <option value="">은행을 선택하세요</option>
                  {#each banks as bank}
                    <option value={bank.code}>{bank.name}</option>
                  {/each}
                </select>
                {#if errors.bankCode}<span class="error-msg">{errors.bankCode}</span>{/if}
              </div>
              <div class="form-group">
                <label for="accountNumber">계좌번호 <span class="required">*</span></label>
                <input
                  id="accountNumber"
                  type="text"
                  bind:value={bankAccount.accountNumber}
                  placeholder="'-' 없이 입력"
                  class="form-input"
                  class:error={errors.accountNumber}
                />
                {#if errors.accountNumber}<span class="error-msg">{errors.accountNumber}</span>{/if}
              </div>
            </div>
            <div class="form-grid">
              <div class="form-group">
                <label for="accountHolder">예금주 <span class="required">*</span></label>
                <input
                  id="accountHolder"
                  type="text"
                  bind:value={bankAccount.accountHolder}
                  placeholder="예금주명"
                  class="form-input"
                  class:error={errors.accountHolder}
                />
                {#if errors.accountHolder}<span class="error-msg">{errors.accountHolder}</span>{/if}
              </div>
            </div>
          </div>
        </div>
      {/if}

      <!-- Step 2: Fee Config -->
      {#if currentStep === 2}
        <div class="form-step">
          <div class="form-section">
            <h3 class="section-title">
              <span class="section-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="1" x2="12" y2="23"/>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
              </span>
              수수료 설정
            </h3>
            <p class="section-desc">결제 유형별 수수료율을 설정합니다. (단위: %)</p>

            {#each Object.entries(feeConfig) as [feeType, config]}
              <div class="fee-card">
                <div class="fee-card-header">
                  <h4>{feeTypeLabels[feeType]}</h4>
                </div>
                <div class="fee-grid">
                  {#each Object.keys(config) as category}
                    <div class="fee-input-group">
                      <label>{feeCategoryLabels[category as keyof FeeConfig]}</label>
                      <div class="input-with-suffix">
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          max="100"
                          bind:value={feeConfig[feeType as keyof typeof feeConfig][category as keyof FeeConfig]}
                          class="form-input fee-input"
                        />
                        <span class="suffix">%</span>
                      </div>
                    </div>
                  {/each}
                </div>
              </div>
            {/each}
          </div>

          <div class="form-section">
            <h3 class="section-title">
              <span class="section-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
                  <path d="M12 6v6l4 2"/>
                </svg>
              </span>
              한도 설정
            </h3>
            <p class="section-desc">거래 한도를 설정합니다. (단위: 백만원)</p>

            <div class="form-grid">
              <div class="form-group">
                <label for="perTransaction">1회 한도 <span class="required">*</span></label>
                <div class="input-with-suffix">
                  <input
                    id="perTransaction"
                    type="number"
                    min="0"
                    bind:value={limitConfig.perTransaction}
                    placeholder="0"
                    class="form-input"
                    class:error={errors.perTransaction}
                  />
                  <span class="suffix">백만원</span>
                </div>
                {#if errors.perTransaction}<span class="error-msg">{errors.perTransaction}</span>{/if}
              </div>
              <div class="form-group">
                <label for="perDay">1일 한도 <span class="required">*</span></label>
                <div class="input-with-suffix">
                  <input
                    id="perDay"
                    type="number"
                    min="0"
                    bind:value={limitConfig.perDay}
                    placeholder="0"
                    class="form-input"
                    class:error={errors.perDay}
                  />
                  <span class="suffix">백만원</span>
                </div>
                {#if errors.perDay}<span class="error-msg">{errors.perDay}</span>{/if}
              </div>
            </div>
          </div>
        </div>
      {/if}

      <!-- Step 3: Confirmation -->
      {#if currentStep === 3}
        <div class="form-step">
          <div class="confirmation-section">
            <h3 class="section-title">
              <span class="section-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
              </span>
              입력 정보 확인
            </h3>
            <p class="section-desc">입력하신 정보를 확인해주세요.</p>

            <div class="summary-card">
              <div class="summary-header">기본 정보</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">영업점 유형</span>
                  <span class="value">{BRANCH_TYPE_LABELS[orgType]}</span>
                </div>
                <div class="summary-item">
                  <span class="label">영업점명</span>
                  <span class="value">{orgName}</span>
                </div>
                <div class="summary-item">
                  <span class="label">사업자 구분</span>
                  <span class="value">{businessTypeLabels[businessInfo.businessType]}</span>
                </div>
                {#if businessInfo.businessNumber}
                  <div class="summary-item">
                    <span class="label">사업자등록번호</span>
                    <span class="value mono">{businessInfo.businessNumber}</span>
                  </div>
                {/if}
                <div class="summary-item">
                  <span class="label">대표자</span>
                  <span class="value">{businessInfo.representative}</span>
                </div>
                <div class="summary-item">
                  <span class="label">사업장 소재지</span>
                  <span class="value">{businessInfo.businessAddress}</span>
                </div>
                {#if businessInfo.mainPhone}
                  <div class="summary-item">
                    <span class="label">대표번호</span>
                    <span class="value">{businessInfo.mainPhone}</span>
                  </div>
                {/if}
                {#if businessInfo.email}
                  <div class="summary-item">
                    <span class="label">이메일</span>
                    <span class="value">{businessInfo.email}</span>
                  </div>
                {/if}
              </div>
            </div>

            <div class="summary-card">
              <div class="summary-header">정산 계좌</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">은행</span>
                  <span class="value">{bankAccount.bankName}</span>
                </div>
                <div class="summary-item">
                  <span class="label">계좌번호</span>
                  <span class="value mono">{bankAccount.accountNumber}</span>
                </div>
                <div class="summary-item">
                  <span class="label">예금주</span>
                  <span class="value">{bankAccount.accountHolder}</span>
                </div>
              </div>
            </div>

            <div class="summary-card">
              <div class="summary-header">수수료 설정</div>
              <div class="fee-summary">
                {#each Object.entries(feeConfig) as [feeType, config]}
                  <div class="fee-summary-row">
                    <span class="fee-type">{feeTypeLabels[feeType]}</span>
                    <div class="fee-values">
                      {#each Object.entries(config) as [category, value]}
                        <span class="fee-value">
                          <span class="category">{feeCategoryLabels[category as keyof FeeConfig]}</span>
                          <span class="rate">{value}%</span>
                        </span>
                      {/each}
                    </div>
                  </div>
                {/each}
              </div>
            </div>

            <div class="summary-card">
              <div class="summary-header">한도 설정</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">1회 한도</span>
                  <span class="value">{limitConfig.perTransaction.toLocaleString()}백만원</span>
                </div>
                <div class="summary-item">
                  <span class="label">1일 한도</span>
                  <span class="value">{limitConfig.perDay.toLocaleString()}백만원</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      {/if}

      <!-- Navigation Buttons -->
      <div class="form-actions">
        {#if currentStep > 1}
          <button class="btn btn-secondary" onclick={handlePrev} disabled={loading}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
            이전
          </button>
        {:else}
          <div></div>
        {/if}

        {#if currentStep < totalSteps}
          <button class="btn btn-primary" onclick={handleNext}>
            다음
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12"/>
              <polyline points="12 5 19 12 12 19"/>
            </svg>
          </button>
        {:else}
          <button class="btn btn-success" onclick={handleSubmit} disabled={loading}>
            {#if loading}
              <span class="btn-spinner"></span>
              등록 중...
            {:else}
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              등록하기
            {/if}
          </button>
        {/if}
      </div>
    {/if}
  </div>
</div>

<style>
  .registration-page {
    max-width: 960px;
    margin: 0 auto;
    padding: 0 1.5rem 3rem;
  }

  /* Page Header */
  .page-header {
    margin-bottom: 2rem;
    padding-bottom: 1.5rem;
    border-bottom: 2px solid #1a1a2e;
  }

  .header-content h1 {
    font-size: 1.875rem;
    font-weight: 800;
    color: #1a1a2e;
    letter-spacing: -0.025em;
    margin: 0;
  }

  .subtitle {
    color: #64748b;
    font-size: 0.9375rem;
    margin: 0.5rem 0 0;
  }

  /* Step Indicator */
  .step-indicator {
    position: relative;
    margin-bottom: 2.5rem;
    padding: 0 2rem;
  }

  .step-line {
    position: absolute;
    top: 20px;
    left: 15%;
    right: 15%;
    height: 4px;
    background: #e2e8f0;
    border-radius: 2px;
    z-index: 0;
  }

  .step-progress {
    height: 100%;
    background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
    border-radius: 2px;
    transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .steps {
    display: flex;
    justify-content: space-between;
    position: relative;
    z-index: 1;
  }

  .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
  }

  .step-circle {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: #fff;
    border: 3px solid #e2e8f0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 1rem;
    color: #94a3b8;
    transition: all 0.3s ease;
  }

  .step.active .step-circle {
    border-color: #6366f1;
    color: #6366f1;
    box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15);
  }

  .step.completed .step-circle {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border-color: transparent;
    color: #fff;
  }

  .step-label {
    font-size: 0.875rem;
    font-weight: 600;
    color: #94a3b8;
    transition: color 0.3s;
  }

  .step.active .step-label,
  .step.completed .step-label {
    color: #1a1a2e;
  }

  /* Form Container */
  .form-container {
    background: #fff;
    border-radius: 1rem;
    box-shadow:
      0 1px 3px rgba(0, 0, 0, 0.04),
      0 8px 32px rgba(0, 0, 0, 0.08);
    border: 1px solid #e2e8f0;
    overflow: hidden;
  }

  .form-step {
    padding: 2rem;
    animation: fadeIn 0.3s ease;
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* Form Section */
  .form-section {
    margin-bottom: 2rem;
    padding-bottom: 2rem;
    border-bottom: 1px solid #f1f5f9;
  }

  .form-section:last-child {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    font-size: 1.125rem;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0 0 1.25rem;
  }

  .section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border-radius: 0.5rem;
    color: #fff;
  }

  .section-desc {
    color: #64748b;
    font-size: 0.875rem;
    margin: -0.5rem 0 1.25rem;
    padding-left: 3rem;
  }

  /* Form Grid */
  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1.25rem;
  }

  .form-grid.cols-3 {
    grid-template-columns: repeat(3, 1fr);
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .form-group.full-width {
    grid-column: 1 / -1;
  }

  .form-group label {
    font-size: 0.875rem;
    font-weight: 600;
    color: #475569;
  }

  .required {
    color: #ef4444;
  }

  /* Form Inputs */
  .form-input,
  .form-select {
    padding: 0.75rem 1rem;
    border: 1.5px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 0.9375rem;
    color: #1a1a2e;
    background: #fff;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .form-input::placeholder {
    color: #94a3b8;
  }

  .form-input:hover,
  .form-select:hover {
    border-color: #cbd5e1;
  }

  .form-input:focus,
  .form-select:focus {
    outline: none;
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
  }

  .form-input.error,
  .form-select.error {
    border-color: #ef4444;
  }

  .form-input.error:focus,
  .form-select.error:focus {
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
  }

  .error-msg {
    font-size: 0.8125rem;
    color: #ef4444;
    margin-top: -0.25rem;
  }

  /* Input with suffix */
  .input-with-suffix {
    position: relative;
    display: flex;
    align-items: center;
  }

  .input-with-suffix .form-input {
    padding-right: 4rem;
    width: 100%;
  }

  .input-with-suffix .suffix {
    position: absolute;
    right: 1rem;
    color: #64748b;
    font-size: 0.875rem;
    font-weight: 500;
    pointer-events: none;
  }

  /* Fee Card */
  .fee-card {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.25rem;
    margin-bottom: 1rem;
  }

  .fee-card:last-child {
    margin-bottom: 0;
  }

  .fee-card-header {
    margin-bottom: 1rem;
  }

  .fee-card-header h4 {
    font-size: 0.9375rem;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0;
  }

  .fee-grid {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 0.75rem;
  }

  .fee-input-group {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }

  .fee-input-group label {
    font-size: 0.75rem;
    font-weight: 600;
    color: #64748b;
    text-align: center;
  }

  .fee-input {
    text-align: center;
    padding: 0.625rem 0.5rem !important;
    padding-right: 1.75rem !important;
  }

  .fee-input-group .input-with-suffix .suffix {
    right: 0.5rem;
    font-size: 0.75rem;
  }

  /* Confirmation Section */
  .confirmation-section {
    padding: 0;
  }

  .summary-card {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    overflow: hidden;
    margin-bottom: 1rem;
  }

  .summary-card:last-child {
    margin-bottom: 0;
  }

  .summary-header {
    background: linear-gradient(180deg, #f1f5f9 0%, #e2e8f0 100%);
    padding: 0.875rem 1.25rem;
    font-weight: 700;
    font-size: 0.875rem;
    color: #475569;
    text-transform: uppercase;
    letter-spacing: 0.025em;
    border-bottom: 1px solid #e2e8f0;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 0;
  }

  .summary-item {
    display: flex;
    justify-content: space-between;
    padding: 0.875rem 1.25rem;
    border-bottom: 1px solid #f1f5f9;
  }

  .summary-item:nth-child(odd) {
    border-right: 1px solid #f1f5f9;
  }

  .summary-item .label {
    font-size: 0.8125rem;
    color: #64748b;
  }

  .summary-item .value {
    font-size: 0.875rem;
    font-weight: 600;
    color: #1a1a2e;
  }

  .summary-item .value.mono {
    font-family: 'JetBrains Mono', 'SF Mono', Monaco, monospace;
    color: #6366f1;
  }

  /* Fee Summary */
  .fee-summary {
    padding: 1rem 1.25rem;
  }

  .fee-summary-row {
    display: flex;
    align-items: flex-start;
    gap: 1rem;
    padding: 0.75rem 0;
    border-bottom: 1px solid #f1f5f9;
  }

  .fee-summary-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  .fee-summary-row:first-child {
    padding-top: 0;
  }

  .fee-type {
    font-size: 0.875rem;
    font-weight: 600;
    color: #1a1a2e;
    min-width: 80px;
  }

  .fee-values {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    flex: 1;
  }

  .fee-value {
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    padding: 0.25rem 0.625rem;
    font-size: 0.75rem;
  }

  .fee-value .category {
    color: #64748b;
  }

  .fee-value .rate {
    font-weight: 600;
    color: #6366f1;
    font-family: 'JetBrains Mono', monospace;
  }

  /* Form Actions */
  .form-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem 2rem;
    background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
    border-top: 1px solid #e2e8f0;
  }

  /* Buttons */
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-size: 0.9375rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    text-decoration: none;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-primary {
    background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
    color: #fff;
  }

  .btn-primary:hover:not(:disabled) {
    background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
  }

  .btn-secondary {
    background: #fff;
    color: #475569;
    border: 1.5px solid #e2e8f0;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #f8fafc;
    border-color: #cbd5e1;
  }

  .btn-success {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: #fff;
  }

  .btn-success:hover:not(:disabled) {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
  }

  .btn-spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top-color: #fff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  /* Result Screen */
  .result-screen {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 2rem;
    text-align: center;
  }

  .result-icon {
    margin-bottom: 1.5rem;
  }

  .result-icon.success {
    color: #10b981;
  }

  .result-icon.error {
    color: #ef4444;
  }

  .result-screen h2 {
    font-size: 1.5rem;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0 0 0.75rem;
  }

  .result-screen p {
    font-size: 1rem;
    color: #64748b;
    margin: 0 0 2rem;
    max-width: 400px;
  }

  .result-actions {
    display: flex;
    gap: 1rem;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .registration-page {
      padding: 0 1rem 2rem;
    }

    .step-indicator {
      padding: 0;
    }

    .step-line {
      left: 10%;
      right: 10%;
    }

    .step-circle {
      width: 36px;
      height: 36px;
      font-size: 0.875rem;
    }

    .step-label {
      font-size: 0.75rem;
    }

    .form-step {
      padding: 1.5rem;
    }

    .form-grid,
    .form-grid.cols-3 {
      grid-template-columns: 1fr;
    }

    .fee-grid {
      grid-template-columns: repeat(3, 1fr);
    }

    .summary-grid {
      grid-template-columns: 1fr;
    }

    .summary-item:nth-child(odd) {
      border-right: none;
    }

    .form-actions {
      padding: 1.25rem 1.5rem;
    }

    .result-actions {
      flex-direction: column;
      width: 100%;
    }

    .result-actions .btn {
      width: 100%;
    }
  }

  @media (max-width: 480px) {
    .fee-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .fee-values {
      gap: 0.375rem;
    }

    .fee-summary-row {
      flex-direction: column;
      gap: 0.5rem;
    }
  }
</style>
