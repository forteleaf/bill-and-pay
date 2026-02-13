<script lang="ts">
  import { branchApi, businessEntityApi } from "@/api/branch";
  import {
    OrgType,
    BusinessType,
    BRANCH_TYPE_LABELS,
    type BranchCreateRequest,
    type BusinessInfo,
    type BankAccountInfo,
    type FeeConfig,
    type LimitConfig,
    type BusinessEntity,
    type BusinessEntityCreateRequest,
  } from "@/types/branch";
  import { formatBusinessNumberInput } from "@/utils/formatters";
  import { Button } from "$lib/components/ui/button";
  import { Card, CardContent } from "$lib/components/ui/card";
  import { Label } from "$lib/components/ui/label";
  import { DatePicker } from "$lib/components/ui/date-picker";
  import * as Select from "$lib/components/ui/select";

  let currentStep = $state(1);
  const totalSteps = 3;

  let orgType = $state<OrgType>(OrgType.DISTRIBUTOR);
  let orgName = $state("");
  let businessInfo = $state<BusinessInfo>({
    businessType: BusinessType.INDIVIDUAL,
    businessNumber: "",
    corporateNumber: "",
    representative: "",
    openDate: "",
    businessAddress: "",
    actualAddress: "",
    businessCategory: "",
    businessType2: "",
    mainPhone: "",
    managerName: "",
    managerPhone: "",
    email: "",
  });
  let bankAccount = $state<BankAccountInfo>({
    bankCode: "",
    bankName: "",
    accountNumber: "",
    accountHolder: "",
  });

  const createDefaultFeeConfig = (): FeeConfig => ({
    general: 0,
    small: 0,
    medium1: 0,
    medium2: 0,
    medium3: 0,
    foreign: 0,
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
    recurring: createDefaultFeeConfig(),
  });

  let limitConfig = $state<LimitConfig>({
    perTransaction: 0,
    perDay: 0,
  });

  let loading = $state(false);
  let errors = $state<Record<string, string>>({});
  let submitResult = $state<{ success: boolean; message: string } | null>(null);

  // Business entity search state
  let selectedBusinessEntity = $state<BusinessEntity | null>(null);
  let businessSearching = $state(false);
  let businessSearched = $state(false);
  let businessSearchError = $state<string | null>(null);

  // Derived state for search button visibility
  let canSearchBusiness = $derived(
    businessInfo.businessNumber?.replace(/-/g, "").length === 10 &&
      !selectedBusinessEntity,
  );

  const banks = [
    { code: "004", name: "KB국민은행" },
    { code: "011", name: "NH농협은행" },
    { code: "020", name: "우리은행" },
    { code: "088", name: "신한은행" },
    { code: "081", name: "하나은행" },
    { code: "003", name: "IBK기업은행" },
    { code: "023", name: "SC제일은행" },
    { code: "027", name: "씨티은행" },
    { code: "039", name: "경남은행" },
    { code: "034", name: "광주은행" },
    { code: "031", name: "대구은행" },
    { code: "032", name: "부산은행" },
    { code: "037", name: "전북은행" },
    { code: "035", name: "제주은행" },
    { code: "090", name: "카카오뱅크" },
    { code: "092", name: "토스뱅크" },
    { code: "089", name: "케이뱅크" },
  ];

  const feeTypeLabels: Record<string, string> = {
    terminal: "단말기",
    oldAuth: "구인증",
    nonAuth: "비인증",
    authPay: "인증결제",
    recurring: "정기과금",
  };

  const feeCategoryLabels: Record<keyof FeeConfig, string> = {
    general: "일반",
    small: "영세",
    medium1: "중소1",
    medium2: "중소2",
    medium3: "중소3",
    foreign: "해외",
  };

  const businessTypeLabels: Record<BusinessType, string> = {
    [BusinessType.INDIVIDUAL]: "개인사업자",
    [BusinessType.CORPORATION]: "법인사업자",
    [BusinessType.NON_BUSINESS]: "비사업자",
  };

  function formatCorporateNumber(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 13);
    if (digits.length <= 6) return digits;
    return `${digits.slice(0, 6)}-${digits.slice(6)}`;
  }

  function handleBusinessNumberInput(e: Event) {
    const input = e.target as HTMLInputElement;
    const formatted = formatBusinessNumberInput(input.value);
    businessInfo.businessNumber = formatted;
    input.value = formatted;
  }

  function handleCorporateNumberInput(e: Event) {
    const input = e.target as HTMLInputElement;
    const formatted = formatCorporateNumber(input.value);
    businessInfo.corporateNumber = formatted;
    input.value = formatted;
  }

  async function searchBusinessEntity() {
    if (
      !businessInfo.businessNumber ||
      businessInfo.businessNumber.replace(/-/g, "").length !== 10
    ) {
      return;
    }

    businessSearching = true;
    businessSearchError = null;
    businessSearched = false;

    try {
      const response = await businessEntityApi.searchByBusinessNumber(
        businessInfo.businessNumber,
      );
      businessSearched = true;

      if (response.success && response.data) {
        selectedBusinessEntity = response.data;
        // Auto-fill form fields from the entity
        selectBusinessEntity(response.data);
      } else {
        selectedBusinessEntity = null;
      }
    } catch (err) {
      businessSearchError =
        err instanceof Error ? err.message : "검색 중 오류가 발생했습니다.";
      selectedBusinessEntity = null;
    } finally {
      businessSearching = false;
    }
  }

  function selectBusinessEntity(entity: BusinessEntity) {
    selectedBusinessEntity = entity;
    // Map BusinessEntity fields to BusinessInfo fields
    businessInfo.businessType = entity.businessType;
    businessInfo.businessNumber = entity.businessNumber || "";
    businessInfo.corporateNumber = entity.corporateNumber || "";
    businessInfo.representative = entity.representativeName;
    businessInfo.openDate = entity.openDate || "";
    businessInfo.businessAddress = entity.businessAddress || "";
    businessInfo.actualAddress = entity.actualAddress || "";
    businessInfo.businessCategory = entity.businessCategory || "";
    businessInfo.businessType2 = entity.businessSubCategory || "";
    businessInfo.mainPhone = entity.mainPhone || "";
    businessInfo.managerName = entity.managerName || "";
    businessInfo.managerPhone = entity.managerPhone || "";
    businessInfo.email = entity.email || "";
  }

  function clearSelectedBusinessEntity() {
    selectedBusinessEntity = null;
    businessSearched = false;
    businessSearchError = null;
    // Clear business info fields for fresh entry
    businessInfo.corporateNumber = "";
    businessInfo.representative = "";
    businessInfo.openDate = "";
    businessInfo.businessAddress = "";
    businessInfo.actualAddress = "";
    businessInfo.businessCategory = "";
    businessInfo.businessType2 = "";
    businessInfo.mainPhone = "";
    businessInfo.managerName = "";
    businessInfo.managerPhone = "";
    businessInfo.email = "";
  }

  function validateStep1(): boolean {
    const newErrors: Record<string, string> = {};

    if (!orgName.trim()) {
      newErrors.orgName = "영업점명을 입력해주세요.";
    }
    if (!businessInfo.representative.trim()) {
      newErrors.representative = "대표자명을 입력해주세요.";
    }
    if (!businessInfo.businessAddress.trim()) {
      newErrors.businessAddress = "사업장 소재지를 입력해주세요.";
    }
    if (
      businessInfo.businessType !== BusinessType.NON_BUSINESS &&
      !businessInfo.businessNumber?.trim()
    ) {
      newErrors.businessNumber = "사업자등록번호를 입력해주세요.";
    }
    if (!bankAccount.bankCode) {
      newErrors.bankCode = "은행을 선택해주세요.";
    }
    if (!bankAccount.accountNumber.trim()) {
      newErrors.accountNumber = "계좌번호를 입력해주세요.";
    }
    if (!bankAccount.accountHolder.trim()) {
      newErrors.accountHolder = "예금주를 입력해주세요.";
    }

    errors = newErrors;
    return Object.keys(newErrors).length === 0;
  }

  function validateStep2(): boolean {
    const newErrors: Record<string, string> = {};

    if (limitConfig.perTransaction <= 0) {
      newErrors.perTransaction = "1회 한도를 입력해주세요.";
    }
    if (limitConfig.perDay <= 0) {
      newErrors.perDay = "1일 한도를 입력해주세요.";
    }
    if (limitConfig.perTransaction > limitConfig.perDay) {
      newErrors.perTransaction = "1회 한도는 1일 한도를 초과할 수 없습니다.";
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

    try {
      let businessEntityId: string;

      if (selectedBusinessEntity) {
        businessEntityId = selectedBusinessEntity.id;
      } else {
        const createEntityRequest: BusinessEntityCreateRequest = {
          businessType: businessInfo.businessType,
          businessNumber: businessInfo.businessNumber || undefined,
          corporateNumber: businessInfo.corporateNumber || undefined,
          businessName: orgName,
          representativeName: businessInfo.representative,
          openDate: businessInfo.openDate || undefined,
          businessAddress: businessInfo.businessAddress || undefined,
          actualAddress: businessInfo.actualAddress || undefined,
          businessCategory: businessInfo.businessCategory || undefined,
          businessSubCategory: businessInfo.businessType2 || undefined,
          mainPhone: businessInfo.mainPhone || undefined,
          managerName: businessInfo.managerName || undefined,
          managerPhone: businessInfo.managerPhone || undefined,
          email: businessInfo.email || undefined,
        };

        const entityResponse =
          await businessEntityApi.create(createEntityRequest);
        if (!entityResponse.success || !entityResponse.data) {
          submitResult = {
            success: false,
            message:
              entityResponse.error?.message ||
              "사업자 등록에 실패했습니다. 다시 시도해주세요.",
          };
          return;
        }
        businessEntityId = entityResponse.data.id;
      }

      const requestData: BranchCreateRequest = {
        name: orgName,
        orgType: orgType,
        businessEntityId: businessEntityId,
        bankAccount: bankAccount,
        feeConfig: feeConfig,
        limitConfig: limitConfig,
      };

      const response = await branchApi.createBranch(requestData);
      if (response.success && response.data) {
        submitResult = {
          success: true,
          message: `영업점 "${response.data.name}"이(가) 성공적으로 등록되었습니다.`,
        };
      } else {
        submitResult = {
          success: false,
          message:
            response.error?.message ||
            "등록에 실패했습니다. 다시 시도해주세요.",
        };
      }
    } catch (err) {
      submitResult = {
        success: false,
        message:
          err instanceof Error ? err.message : "네트워크 오류가 발생했습니다.",
      };
    } finally {
      loading = false;
    }
  }

  function handleBankSelect(code: string) {
    const selected = banks.find((b) => b.code === code);
    if (selected) {
      bankAccount.bankCode = selected.code;
      bankAccount.bankName = selected.name;
    }
  }

  function resetForm() {
    currentStep = 1;
    orgType = OrgType.DISTRIBUTOR;
    orgName = "";
    businessInfo = {
      businessType: BusinessType.INDIVIDUAL,
      businessNumber: "",
      corporateNumber: "",
      representative: "",
      openDate: "",
      businessAddress: "",
      actualAddress: "",
      businessCategory: "",
      businessType2: "",
      mainPhone: "",
      managerName: "",
      managerPhone: "",
      email: "",
    };
    bankAccount = {
      bankCode: "",
      bankName: "",
      accountNumber: "",
      accountHolder: "",
    };
    feeConfig = {
      terminal: createDefaultFeeConfig(),
      oldAuth: createDefaultFeeConfig(),
      nonAuth: createDefaultFeeConfig(),
      authPay: createDefaultFeeConfig(),
      recurring: createDefaultFeeConfig(),
    };
    limitConfig = { perTransaction: 0, perDay: 0 };
    errors = {};
    submitResult = null;
    // Reset business entity search state
    selectedBusinessEntity = null;
    businessSearching = false;
    businessSearched = false;
    businessSearchError = null;
  }
</script>

<div class="max-w-[960px] mx-auto px-6 pb-12">
  <div class="mb-8 pb-6 border-b-2 border-foreground">
    <h1 class="text-3xl font-extrabold text-foreground tracking-tight">
      영업점 등록
    </h1>
    <p class="text-muted-foreground text-sm mt-2">새로운 영업점을 등록합니다</p>
  </div>

  <div class="relative mb-10 px-8">
    <div
      class="absolute top-5 left-[15%] right-[15%] h-1 bg-muted rounded-full z-0"
    >
      <div
        class="h-full bg-gradient-to-r from-primary to-violet-500 rounded-full transition-all duration-400"
        style="width: {((currentStep - 1) / (totalSteps - 1)) * 100}%"
      ></div>
    </div>
    <div class="flex justify-between relative z-10">
      {#each [1, 2, 3] as step}
        <div class="flex flex-col items-center gap-3">
          <div
            class="w-10 h-10 rounded-full bg-background border-3 flex items-center justify-center font-bold text-base transition-all {currentStep ===
            step
              ? 'border-primary text-primary shadow-[0_0_0_4px_rgba(99,102,241,0.15)]'
              : currentStep > step
                ? 'bg-gradient-to-br from-primary to-violet-500 border-transparent text-white'
                : 'border-muted text-muted-foreground'}"
          >
            {#if currentStep > step}
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="3"
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
            {:else}
              {step}
            {/if}
          </div>
          <span
            class="text-sm font-semibold transition-colors {currentStep >= step
              ? 'text-foreground'
              : 'text-muted-foreground'}"
          >
            {#if step === 1}기본정보{:else if step === 2}수수료설정{:else}확인/등록{/if}
          </span>
        </div>
      {/each}
    </div>
  </div>

  <Card class="shadow-lg">
    {#if submitResult}
      <CardContent class="py-16 px-8">
        <div class="flex flex-col items-center justify-center text-center">
          <div
            class="mb-6 {submitResult.success
              ? 'text-emerald-500'
              : 'text-destructive'}"
          >
            {#if submitResult.success}
              <svg
                width="64"
                height="64"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="16 10 10 16 8 14" />
              </svg>
            {:else}
              <svg
                width="64"
                height="64"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
            {/if}
          </div>
          <h2 class="text-2xl font-bold text-foreground mb-3">
            {submitResult.success ? "등록 완료" : "등록 실패"}
          </h2>
          <p class="text-muted-foreground mb-8 max-w-[400px]">
            {submitResult.message}
          </p>
          <div class="flex gap-4">
            {#if submitResult.success}
              <Button variant="outline" onclick={resetForm}
                >새 영업점 등록</Button
              >
              <a
                href="/branch"
                class="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-md text-sm font-semibold bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
                >영업점 목록으로</a
              >
            {:else}
              <Button onclick={() => (submitResult = null)}>다시 시도</Button>
            {/if}
          </div>
        </div>
      </CardContent>
    {:else}
      <CardContent
        class="p-8 animate-in fade-in slide-in-from-bottom-2 duration-300"
      >
        {#if currentStep === 1}
          <div class="space-y-8">
            <div class="pb-8 border-b border-border">
              <h3
                class="flex items-center gap-3 text-lg font-bold text-foreground mb-5"
              >
                <span
                  class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white"
                >
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                </span>
                기본 정보
              </h3>
              <div class="grid grid-cols-2 gap-5">
                <div class="flex flex-col gap-2">
                  <Label>영업점 유형 <span class="text-destructive">*</span></Label>
                  <Select.Root type="single" bind:value={orgType}>
                    <Select.Trigger class="w-full">
                      {#if orgType}
                        {BRANCH_TYPE_LABELS[orgType as OrgType] || orgType}
                      {:else}
                        <span class="text-muted-foreground">유형 선택</span>
                      {/if}
                    </Select.Trigger>
                    <Select.Content>
                      {#each Object.values(OrgType) as type}
                        <Select.Item value={type}>{BRANCH_TYPE_LABELS[type]}</Select.Item>
                      {/each}
                    </Select.Content>
                  </Select.Root>
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="orgName"
                    >영업점명 <span class="text-destructive">*</span></Label
                  >
                  <input
                    id="orgName"
                    type="text"
                    bind:value={orgName}
                    placeholder="영업점명을 입력하세요"
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.orgName
                      ? 'border-destructive focus:ring-destructive/20'
                      : ''}"
                  />
                  {#if errors.orgName}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.orgName}</span
                    >{/if}
                </div>
              </div>
            </div>

            <div class="pb-8 border-b border-border">
              <h3
                class="flex items-center gap-3 text-lg font-bold text-foreground mb-5"
              >
                <span
                  class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white"
                >
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                  </svg>
                </span>
                사업자 정보
              </h3>
              <div class="grid grid-cols-3 gap-5 mb-5">
                <div class="flex flex-col gap-2">
                  <Label for="businessType"
                    >사업자 구분 <span class="text-destructive">*</span></Label
                  >
                  <Select.Root type="single" bind:value={businessInfo.businessType} disabled={!!selectedBusinessEntity}>
                    <Select.Trigger class="w-full {selectedBusinessEntity ? 'bg-muted cursor-not-allowed' : ''}">
                      {#if businessInfo.businessType}
                        {businessTypeLabels[businessInfo.businessType as BusinessType] || businessInfo.businessType}
                      {:else}
                        <span class="text-muted-foreground">사업자 구분 선택</span>
                      {/if}
                    </Select.Trigger>
                    <Select.Content>
                      {#each Object.values(BusinessType) as type}
                        <Select.Item value={type}>{businessTypeLabels[type]}</Select.Item>
                      {/each}
                    </Select.Content>
                  </Select.Root>
                </div>
                {#if businessInfo.businessType !== BusinessType.NON_BUSINESS}
                  <div class="flex flex-col gap-2 col-span-2">
                    <Label for="businessNumber"
                      >사업자등록번호 <span class="text-destructive">*</span
                      ></Label
                    >
                    <div class="flex gap-2">
                      <input
                        id="businessNumber"
                        type="text"
                        value={businessInfo.businessNumber}
                        oninput={handleBusinessNumberInput}
                        placeholder="000-00-00000"
                        maxlength="12"
                        disabled={!!selectedBusinessEntity}
                        class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm font-mono focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all flex-1 {errors.businessNumber
                          ? 'border-destructive focus:ring-destructive/20'
                          : ''} {selectedBusinessEntity
                          ? 'bg-muted cursor-not-allowed'
                          : ''}"
                      />
                      {#if canSearchBusiness}
                        <Button
                          type="button"
                          variant="outline"
                          onclick={searchBusinessEntity}
                          disabled={businessSearching}
                          class="h-11 px-4 shrink-0"
                        >
                          {#if businessSearching}
                            <span
                              class="w-4 h-4 border-2 border-primary/30 border-t-primary rounded-full animate-spin"
                            ></span>
                          {:else}
                            <svg
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              stroke-width="2"
                            >
                              <circle cx="11" cy="11" r="8" />
                              <path d="m21 21-4.35-4.35" />
                            </svg>
                          {/if}
                          검색
                        </Button>
                      {/if}
                      {#if selectedBusinessEntity}
                        <Button
                          type="button"
                          variant="outline"
                          onclick={clearSelectedBusinessEntity}
                          class="h-11 px-4 shrink-0"
                        >
                          <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                          >
                            <path d="M12 5v14M5 12h14" />
                          </svg>
                          새 사업자 등록
                        </Button>
                      {/if}
                    </div>
                    {#if errors.businessNumber}<span
                        class="text-xs text-destructive"
                        >{errors.businessNumber}</span
                      >{/if}
                    {#if businessSearchError}<span
                        class="text-xs text-destructive"
                        >{businessSearchError}</span
                      >{/if}
                  </div>
                {/if}
              </div>

              <!-- Business Entity Search Results -->
              {#if businessSearched && !selectedBusinessEntity && !businessSearching}
                <div
                  class="mb-5 p-4 rounded-lg border-2 border-dashed border-amber-300 bg-amber-50/50"
                >
                  <div class="flex items-center gap-2 text-amber-700">
                    <svg
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    <span class="text-sm font-medium"
                      >등록된 사업자가 없습니다. 새로 등록합니다.</span
                    >
                  </div>
                </div>
              {/if}

              {#if selectedBusinessEntity}
                <div
                  class="mb-5 p-4 rounded-lg border border-emerald-200 bg-emerald-50/50"
                >
                  <div class="flex items-start justify-between gap-4">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-2">
                        <svg
                          width="20"
                          height="20"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          class="text-emerald-600"
                        >
                          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                          <polyline points="22 4 12 14.01 9 11.01" />
                        </svg>
                        <span class="text-sm font-semibold text-emerald-700"
                          >기존 사업자 정보 사용</span
                        >
                      </div>
                      <div class="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
                        <div class="flex gap-2">
                          <span class="text-muted-foreground">상호:</span>
                          <span class="font-medium text-foreground"
                            >{selectedBusinessEntity.businessName}</span
                          >
                        </div>
                        <div class="flex gap-2">
                          <span class="text-muted-foreground">대표자:</span>
                          <span class="font-medium text-foreground"
                            >{selectedBusinessEntity.representativeName}</span
                          >
                        </div>
                        {#if selectedBusinessEntity.businessAddress}
                          <div class="flex gap-2 col-span-2">
                            <span class="text-muted-foreground">주소:</span>
                            <span class="font-medium text-foreground"
                              >{selectedBusinessEntity.businessAddress}</span
                            >
                          </div>
                        {/if}
                      </div>
                    </div>
                  </div>
                </div>
              {/if}

              <div class="grid grid-cols-3 gap-5 mb-5">
                {#if businessInfo.businessType === BusinessType.CORPORATION}
                  <div class="flex flex-col gap-2">
                    <Label for="corporateNumber">법인등록번호</Label>
                    <input
                      id="corporateNumber"
                      type="text"
                      value={businessInfo.corporateNumber}
                      oninput={handleCorporateNumberInput}
                      placeholder="000000-0000000"
                      maxlength="14"
                      disabled={!!selectedBusinessEntity}
                      class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm font-mono focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                        ? 'bg-muted cursor-not-allowed'
                        : ''}"
                    />
                  </div>
                {/if}
              </div>
              <div class="grid grid-cols-2 gap-5 mb-5">
                <div class="flex flex-col gap-2">
                  <Label for="representative"
                    >대표자 <span class="text-destructive">*</span></Label
                  >
                  <input
                    id="representative"
                    type="text"
                    bind:value={businessInfo.representative}
                    placeholder="대표자명"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.representative
                      ? 'border-destructive focus:ring-destructive/20'
                      : ''} {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                  {#if errors.representative}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.representative}</span
                    >{/if}
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="openDate">개업연월일</Label>
                  <DatePicker
                    value={businessInfo.openDate}
                    onchange={(d) => (businessInfo.openDate = d)}
                    disabled={!!selectedBusinessEntity}
                    placeholder="개업일 선택"
                    class={selectedBusinessEntity
                      ? "bg-muted cursor-not-allowed"
                      : ""}
                  />
                </div>
              </div>
              <div class="grid grid-cols-1 gap-5 mb-5">
                <div class="flex flex-col gap-2">
                  <Label for="businessAddress"
                    >사업장 소재지 <span class="text-destructive">*</span
                    ></Label
                  >
                  <input
                    id="businessAddress"
                    type="text"
                    bind:value={businessInfo.businessAddress}
                    placeholder="사업장 주소를 입력하세요"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.businessAddress
                      ? 'border-destructive focus:ring-destructive/20'
                      : ''} {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                  {#if errors.businessAddress}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.businessAddress}</span
                    >{/if}
                </div>
              </div>
              <div class="grid grid-cols-1 gap-5 mb-5">
                <div class="flex flex-col gap-2">
                  <Label for="actualAddress">실사업장 소재지</Label>
                  <input
                    id="actualAddress"
                    type="text"
                    bind:value={businessInfo.actualAddress}
                    placeholder="실사업장 주소 (다를 경우)"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
              </div>
              <div class="grid grid-cols-3 gap-5 mb-5">
                <div class="flex flex-col gap-2">
                  <Label for="businessCategory">업태</Label>
                  <input
                    id="businessCategory"
                    type="text"
                    bind:value={businessInfo.businessCategory}
                    placeholder="예: 서비스업"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="businessType2">업종</Label>
                  <input
                    id="businessType2"
                    type="text"
                    bind:value={businessInfo.businessType2}
                    placeholder="예: 소프트웨어 개발"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="mainPhone">대표번호</Label>
                  <input
                    id="mainPhone"
                    type="tel"
                    bind:value={businessInfo.mainPhone}
                    placeholder="02-0000-0000"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
              </div>
              <div class="grid grid-cols-3 gap-5">
                <div class="flex flex-col gap-2">
                  <Label for="managerName">담당자</Label>
                  <input
                    id="managerName"
                    type="text"
                    bind:value={businessInfo.managerName}
                    placeholder="담당자명"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="managerPhone">담당자 연락처</Label>
                  <input
                    id="managerPhone"
                    type="tel"
                    bind:value={businessInfo.managerPhone}
                    placeholder="010-0000-0000"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="email">이메일</Label>
                  <input
                    id="email"
                    type="email"
                    bind:value={businessInfo.email}
                    placeholder="example@email.com"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                </div>
              </div>
            </div>

            <div>
              <h3
                class="flex items-center gap-3 text-lg font-bold text-foreground mb-5"
              >
                <span
                  class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white"
                >
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
                    <line x1="1" y1="10" x2="23" y2="10" />
                  </svg>
                </span>
                정산 계좌 정보
              </h3>
              <div class="grid grid-cols-2 gap-5 mb-5">
                <div class="flex flex-col gap-2">
                  <Label for="bankCode"
                    >은행 <span class="text-destructive">*</span></Label
                  >
                  <Select.Root type="single" value={bankAccount.bankCode} onValueChange={(v) => handleBankSelect(v)}>
                    <Select.Trigger class="w-full {errors.bankCode ? 'border-destructive' : ''}">
                      {#if bankAccount.bankCode}
                        {banks.find(b => b.code === bankAccount.bankCode)?.name || bankAccount.bankCode}
                      {:else}
                        <span class="text-muted-foreground">은행을 선택하세요</span>
                      {/if}
                    </Select.Trigger>
                    <Select.Content>
                      {#each banks as bank}
                        <Select.Item value={bank.code}>{bank.name}</Select.Item>
                      {/each}
                    </Select.Content>
                  </Select.Root>
                  {#if errors.bankCode}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.bankCode}</span
                    >{/if}
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="accountNumber"
                    >계좌번호 <span class="text-destructive">*</span></Label
                  >
                  <input
                    id="accountNumber"
                    type="text"
                    bind:value={bankAccount.accountNumber}
                    placeholder="'-' 없이 입력"
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.accountNumber
                      ? 'border-destructive focus:ring-destructive/20'
                      : ''}"
                  />
                  {#if errors.accountNumber}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.accountNumber}</span
                    >{/if}
                </div>
              </div>
              <div class="grid grid-cols-2 gap-5">
                <div class="flex flex-col gap-2">
                  <Label for="accountHolder"
                    >예금주 <span class="text-destructive">*</span></Label
                  >
                  <input
                    id="accountHolder"
                    type="text"
                    bind:value={bankAccount.accountHolder}
                    placeholder="예금주명"
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.accountHolder
                      ? 'border-destructive focus:ring-destructive/20'
                      : ''}"
                  />
                  {#if errors.accountHolder}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.accountHolder}</span
                    >{/if}
                </div>
              </div>
            </div>
          </div>
        {/if}

        {#if currentStep === 2}
          <div class="space-y-8">
            <div class="pb-8 border-b border-border">
              <h3
                class="flex items-center gap-3 text-lg font-bold text-foreground mb-2"
              >
                <span
                  class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white"
                >
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <line x1="12" y1="1" x2="12" y2="23" />
                    <path
                      d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"
                    />
                  </svg>
                </span>
                수수료 설정
              </h3>
              <p class="text-muted-foreground text-sm mb-5 pl-12">
                결제 유형별 수수료율을 설정합니다. (단위: %)
              </p>

              {#each Object.entries(feeConfig) as [feeType, config]}
                <div
                  class="bg-muted/50 border border-border rounded-xl p-5 mb-4 last:mb-0"
                >
                  <h4 class="text-sm font-bold text-foreground mb-4">
                    {feeTypeLabels[feeType]}
                  </h4>
                  <div class="grid grid-cols-6 gap-3">
                    {#each Object.keys(config) as category}
                      <div class="flex flex-col gap-1.5">
                        <span
                          class="text-xs font-semibold text-muted-foreground text-center"
                          >{feeCategoryLabels[
                            category as keyof FeeConfig
                          ]}</span
                        >
                        <div class="relative">
                          <input
                            type="number"
                            step="0.01"
                            min="0"
                            max="100"
                            bind:value={
                              feeConfig[feeType as keyof typeof feeConfig][
                                category as keyof FeeConfig
                              ]
                            }
                            class="h-10 w-full px-2 pr-6 rounded-md border-[1.5px] border-input bg-background text-sm text-center focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all"
                          />
                          <span
                            class="absolute right-2 top-1/2 -translate-y-1/2 text-xs text-muted-foreground pointer-events-none"
                            >%</span
                          >
                        </div>
                      </div>
                    {/each}
                  </div>
                </div>
              {/each}
            </div>

            <div>
              <h3
                class="flex items-center gap-3 text-lg font-bold text-foreground mb-2"
              >
                <span
                  class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white"
                >
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"
                    />
                    <path d="M12 6v6l4 2" />
                  </svg>
                </span>
                한도 설정
              </h3>
              <p class="text-muted-foreground text-sm mb-5 pl-12">
                거래 한도를 설정합니다. (단위: 백만원)
              </p>

              <div class="grid grid-cols-2 gap-5">
                <div class="flex flex-col gap-2">
                  <Label for="perTransaction"
                    >1회 한도 <span class="text-destructive">*</span></Label
                  >
                  <div class="relative">
                    <input
                      id="perTransaction"
                      type="number"
                      min="0"
                      bind:value={limitConfig.perTransaction}
                      placeholder="0"
                      class="h-11 w-full px-4 pr-16 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.perTransaction
                        ? 'border-destructive focus:ring-destructive/20'
                        : ''}"
                    />
                    <span
                      class="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-muted-foreground pointer-events-none"
                      >백만원</span
                    >
                  </div>
                  {#if errors.perTransaction}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.perTransaction}</span
                    >{/if}
                </div>
                <div class="flex flex-col gap-2">
                  <Label for="perDay"
                    >1일 한도 <span class="text-destructive">*</span></Label
                  >
                  <div class="relative">
                    <input
                      id="perDay"
                      type="number"
                      min="0"
                      bind:value={limitConfig.perDay}
                      placeholder="0"
                      class="h-11 w-full px-4 pr-16 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.perDay
                        ? 'border-destructive focus:ring-destructive/20'
                        : ''}"
                    />
                    <span
                      class="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-muted-foreground pointer-events-none"
                      >백만원</span
                    >
                  </div>
                  {#if errors.perDay}<span
                      class="text-xs text-destructive -mt-1"
                      >{errors.perDay}</span
                    >{/if}
                </div>
              </div>
            </div>
          </div>
        {/if}

        {#if currentStep === 3}
          <div>
            <h3
              class="flex items-center gap-3 text-lg font-bold text-foreground mb-2"
            >
              <span
                class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white"
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                  />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                  <polyline points="10 9 9 9 8 9" />
                </svg>
              </span>
              입력 정보 확인
            </h3>
            <p class="text-muted-foreground text-sm mb-5 pl-12">
              입력하신 정보를 확인해주세요.
            </p>

            <div
              class="bg-muted/50 border border-border rounded-xl overflow-hidden mb-4"
            >
              <div
                class="bg-gradient-to-b from-muted/80 to-muted px-5 py-3.5 font-bold text-xs text-muted-foreground uppercase tracking-wide border-b border-border flex items-center justify-between"
              >
                <span>기본 정보</span>
                {#if selectedBusinessEntity}
                  <span
                    class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-xs font-medium normal-case tracking-normal"
                  >
                    <svg
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                      <polyline points="22 4 12 14.01 9 11.01" />
                    </svg>
                    기존 사업자
                  </span>
                {:else}
                  <span
                    class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 text-xs font-medium normal-case tracking-normal"
                  >
                    <svg
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path d="M12 5v14M5 12h14" />
                    </svg>
                    신규 사업자
                  </span>
                {/if}
              </div>
              <div class="grid grid-cols-2">
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50 border-r border-border/50"
                >
                  <span class="text-xs text-muted-foreground">영업점 유형</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{BRANCH_TYPE_LABELS[orgType]}</span
                  >
                </div>
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50"
                >
                  <span class="text-xs text-muted-foreground">영업점명</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{orgName}</span
                  >
                </div>
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50 border-r border-border/50"
                >
                  <span class="text-xs text-muted-foreground">사업자 구분</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{businessTypeLabels[businessInfo.businessType]}</span
                  >
                </div>
                {#if businessInfo.businessNumber}
                  <div
                    class="flex justify-between px-5 py-3.5 border-b border-border/50"
                  >
                    <span class="text-xs text-muted-foreground"
                      >사업자등록번호</span
                    >
                    <span class="text-sm font-semibold font-mono text-primary"
                      >{businessInfo.businessNumber}</span
                    >
                  </div>
                {/if}
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50 border-r border-border/50"
                >
                  <span class="text-xs text-muted-foreground">대표자</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{businessInfo.representative}</span
                  >
                </div>
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50"
                >
                  <span class="text-xs text-muted-foreground"
                    >사업장 소재지</span
                  >
                  <span class="text-sm font-semibold text-foreground"
                    >{businessInfo.businessAddress}</span
                  >
                </div>
                {#if businessInfo.mainPhone}
                  <div
                    class="flex justify-between px-5 py-3.5 border-b border-border/50 border-r border-border/50"
                  >
                    <span class="text-xs text-muted-foreground">대표번호</span>
                    <span class="text-sm font-semibold text-foreground"
                      >{businessInfo.mainPhone}</span
                    >
                  </div>
                {/if}
                {#if businessInfo.email}
                  <div class="flex justify-between px-5 py-3.5">
                    <span class="text-xs text-muted-foreground">이메일</span>
                    <span class="text-sm font-semibold text-foreground"
                      >{businessInfo.email}</span
                    >
                  </div>
                {/if}
              </div>
            </div>

            <div
              class="bg-muted/50 border border-border rounded-xl overflow-hidden mb-4"
            >
              <div
                class="bg-gradient-to-b from-muted/80 to-muted px-5 py-3.5 font-bold text-xs text-muted-foreground uppercase tracking-wide border-b border-border"
              >
                정산 계좌
              </div>
              <div class="grid grid-cols-2">
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50 border-r border-border/50"
                >
                  <span class="text-xs text-muted-foreground">은행</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{bankAccount.bankName}</span
                  >
                </div>
                <div
                  class="flex justify-between px-5 py-3.5 border-b border-border/50"
                >
                  <span class="text-xs text-muted-foreground">계좌번호</span>
                  <span class="text-sm font-semibold font-mono text-primary"
                    >{bankAccount.accountNumber}</span
                  >
                </div>
                <div
                  class="flex justify-between px-5 py-3.5 border-r border-border/50"
                >
                  <span class="text-xs text-muted-foreground">예금주</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{bankAccount.accountHolder}</span
                  >
                </div>
              </div>
            </div>

            <div
              class="bg-muted/50 border border-border rounded-xl overflow-hidden mb-4"
            >
              <div
                class="bg-gradient-to-b from-muted/80 to-muted px-5 py-3.5 font-bold text-xs text-muted-foreground uppercase tracking-wide border-b border-border"
              >
                수수료 설정
              </div>
              <div class="p-5">
                {#each Object.entries(feeConfig) as [feeType, config]}
                  <div
                    class="flex items-start gap-4 py-3 border-b border-border/50 last:border-b-0 first:pt-0 last:pb-0"
                  >
                    <span class="text-sm font-semibold text-foreground min-w-20"
                      >{feeTypeLabels[feeType]}</span
                    >
                    <div class="flex flex-wrap gap-2 flex-1">
                      {#each Object.entries(config) as [category, value]}
                        <span
                          class="inline-flex items-center gap-1.5 bg-background border border-border rounded-md px-2.5 py-1 text-xs"
                        >
                          <span class="text-muted-foreground"
                            >{feeCategoryLabels[
                              category as keyof FeeConfig
                            ]}</span
                          >
                          <span class="font-semibold font-mono text-primary"
                            >{value}%</span
                          >
                        </span>
                      {/each}
                    </div>
                  </div>
                {/each}
              </div>
            </div>

            <div
              class="bg-muted/50 border border-border rounded-xl overflow-hidden"
            >
              <div
                class="bg-gradient-to-b from-muted/80 to-muted px-5 py-3.5 font-bold text-xs text-muted-foreground uppercase tracking-wide border-b border-border"
              >
                한도 설정
              </div>
              <div class="grid grid-cols-2">
                <div
                  class="flex justify-between px-5 py-3.5 border-r border-border/50"
                >
                  <span class="text-xs text-muted-foreground">1회 한도</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{limitConfig.perTransaction.toLocaleString()}백만원</span
                  >
                </div>
                <div class="flex justify-between px-5 py-3.5">
                  <span class="text-xs text-muted-foreground">1일 한도</span>
                  <span class="text-sm font-semibold text-foreground"
                    >{limitConfig.perDay.toLocaleString()}백만원</span
                  >
                </div>
              </div>
            </div>
          </div>
        {/if}
      </CardContent>

      <div
        class="flex justify-between items-center px-8 py-6 bg-gradient-to-b from-muted/50 to-muted border-t border-border"
      >
        {#if currentStep > 1}
          <Button variant="outline" onclick={handlePrev} disabled={loading}>
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            이전
          </Button>
        {:else}
          <div></div>
        {/if}

        {#if currentStep < totalSteps}
          <Button onclick={handleNext}>
            다음
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </Button>
        {:else}
          <Button
            onclick={handleSubmit}
            disabled={loading}
            class="bg-emerald-500 hover:bg-emerald-600"
          >
            {#if loading}
              <span
                class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"
              ></span>
              등록 중...
            {:else}
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
              등록하기
            {/if}
          </Button>
        {/if}
      </div>
    {/if}
  </Card>
</div>
