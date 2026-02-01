<script lang="ts">
  import { merchantApi } from "../../lib/merchantApi";
  import { businessEntityApi } from "../../lib/branchApi";
  import {
    MerchantBusinessType,
    MERCHANT_BUSINESS_TYPE_LABELS,
    type MerchantCreateRequest,
    type AccessibleOrganization,
    type BlacklistCheckResponse,
  } from "../../types/merchant";
  import type { BusinessEntity } from "../../types/branch";
  import { Button } from "$lib/components/ui/button";
  import { Card, CardContent } from "$lib/components/ui/card";
  import { Label } from "$lib/components/ui/label";
  import { Badge } from "$lib/components/ui/badge";

  let businessType = $state<MerchantBusinessType>(
    MerchantBusinessType.INDIVIDUAL,
  );
  let merchantCode = $state("");
  let name = $state("");
  let organizationId = $state("");
  let businessNumber = $state("");
  let corporateNumber = $state("");
  let representative = $state("");
  let address = $state("");
  let contactName = $state("");
  let contactEmail = $state("");
  let contactPhone = $state("");

  let organizations = $state<AccessibleOrganization[]>([]);
  let loadingOrgs = $state(false);

  let selectedBusinessEntity = $state<BusinessEntity | null>(null);
  let businessSearching = $state(false);
  let businessSearched = $state(false);
  let businessSearchError = $state<string | null>(null);

  let blacklistResult = $state<BlacklistCheckResponse | null>(null);
  let blacklistChecking = $state(false);
  let blacklistError = $state<string | null>(null);

  let loading = $state(false);
  let errors = $state<Record<string, string>>({});
  let submitResult = $state<{ success: boolean; message: string } | null>(null);

  let canSearchBusiness = $derived(
    businessNumber.replace(/-/g, "").length === 10 && !selectedBusinessEntity,
  );

  let canCheckBlacklist = $derived(
    businessNumber.replace(/-/g, "").length === 10 && !blacklistResult,
  );

  let isBlacklisted = $derived(blacklistResult?.isBlacklisted === true);

  $effect(() => {
    generateMerchantCode();
    loadOrganizations();
  });

  function generateMerchantCode() {
    const now = new Date();
    const timestamp = now.toISOString().slice(0, 10).replace(/-/g, "");
    const random = Math.floor(1000 + Math.random() * 9000);
    merchantCode = `MER-${timestamp}-${random}`;
  }

  async function loadOrganizations() {
    loadingOrgs = true;
    try {
      const response = await merchantApi.getAccessibleOrganizations();
      if (response.success && response.data) {
        organizations = response.data;
      }
    } catch (err) {
      console.error("Failed to load organizations:", err);
    } finally {
      loadingOrgs = false;
    }
  }

  function formatBusinessNumber(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 10);
    if (digits.length <= 3) return digits;
    if (digits.length <= 5) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
  }

  function formatCorporateNumber(value: string): string {
    const digits = value.replace(/\D/g, "").slice(0, 13);
    if (digits.length <= 6) return digits;
    return `${digits.slice(0, 6)}-${digits.slice(6)}`;
  }

  function handleBusinessNumberInput(e: Event) {
    const input = e.target as HTMLInputElement;
    const formatted = formatBusinessNumber(input.value);
    businessNumber = formatted;
    input.value = formatted;
    blacklistResult = null;
    blacklistError = null;
  }

  function handleCorporateNumberInput(e: Event) {
    const input = e.target as HTMLInputElement;
    const formatted = formatCorporateNumber(input.value);
    corporateNumber = formatted;
    input.value = formatted;
  }

  async function searchBusinessEntity() {
    if (!businessNumber || businessNumber.replace(/-/g, "").length !== 10) {
      return;
    }

    businessSearching = true;
    businessSearchError = null;
    businessSearched = false;

    try {
      const response =
        await businessEntityApi.searchByBusinessNumber(businessNumber);
      businessSearched = true;

      if (response.success && response.data) {
        selectedBusinessEntity = response.data;
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
    businessType = entity.businessType;
    businessNumber = entity.businessNumber || "";
    corporateNumber = entity.corporateNumber || "";
    representative = entity.representativeName;
    address = entity.businessAddress || "";
    name = entity.businessName;
    contactName = entity.managerName || "";
    contactPhone = entity.managerPhone || "";
    contactEmail = entity.email || "";
  }

  function clearSelectedBusinessEntity() {
    selectedBusinessEntity = null;
    businessSearched = false;
    businessSearchError = null;
    corporateNumber = "";
    representative = "";
    address = "";
    name = "";
    contactName = "";
    contactPhone = "";
    contactEmail = "";
    blacklistResult = null;
    blacklistError = null;
  }

  async function checkBlacklist() {
    if (!businessNumber || businessNumber.replace(/-/g, "").length !== 10) {
      return;
    }

    blacklistChecking = true;
    blacklistError = null;

    try {
      const response = await merchantApi.checkBlacklist(businessNumber);
      if (response.success && response.data) {
        blacklistResult = response.data;
      } else {
        blacklistError = "블랙리스트 조회에 실패했습니다.";
      }
    } catch (err) {
      blacklistError =
        err instanceof Error
          ? err.message
          : "블랙리스트 조회 중 오류가 발생했습니다.";
    } finally {
      blacklistChecking = false;
    }
  }

  function handleBusinessTypeChange(type: MerchantBusinessType) {
    businessType = type;
    clearSelectedBusinessEntity();
    businessNumber = "";
  }

  function validateForm(): boolean {
    const newErrors: Record<string, string> = {};

    if (!merchantCode.trim()) {
      newErrors.merchantCode = "가맹점 코드를 입력해주세요.";
    }
    if (!name.trim()) {
      newErrors.name = "상호를 입력해주세요.";
    }
    if (!organizationId) {
      newErrors.organizationId = "영업점을 선택해주세요.";
    }
    if (businessType === MerchantBusinessType.CORPORATION) {
      if (!businessNumber?.trim()) {
        newErrors.businessNumber = "사업자등록번호를 입력해주세요.";
      }
      if (!corporateNumber?.trim()) {
        newErrors.corporateNumber = "법인등록번호를 입력해주세요.";
      }
    }
    if (businessType === MerchantBusinessType.INDIVIDUAL) {
      if (!businessNumber?.trim()) {
        newErrors.businessNumber = "사업자등록번호를 입력해주세요.";
      }
    }
    if (businessType === MerchantBusinessType.NON_BUSINESS) {
      if (!name?.trim()) {
        newErrors.name = "상호를 입력해주세요.";
      }
      if (!representative?.trim()) {
        newErrors.representative = "대표자를 입력해주세요.";
      }
      if (!address?.trim()) {
        newErrors.address = "주소를 입력해주세요.";
      }
    }
    if (contactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactEmail)) {
      newErrors.contactEmail = "올바른 이메일 형식이 아닙니다.";
    }
    if (isBlacklisted) {
      newErrors.blacklist =
        "블랙리스트에 등록된 사업자는 가맹점 등록이 불가합니다.";
    }

    errors = newErrors;
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit() {
    if (!validateForm()) {
      return;
    }

    loading = true;
    submitResult = null;

    const requestData: MerchantCreateRequest = {
      merchantCode: merchantCode,
      name: name,
      organizationId: organizationId,
      businessType: businessType,
      businessNumber:
        businessType !== MerchantBusinessType.NON_BUSINESS
          ? businessNumber.replace(/-/g, "")
          : undefined,
      corporateNumber:
        businessType === MerchantBusinessType.CORPORATION
          ? corporateNumber.replace(/-/g, "")
          : undefined,
      representative: representative || undefined,
      address: address || undefined,
      contactName: contactName || undefined,
      contactEmail: contactEmail || undefined,
      contactPhone: contactPhone || undefined,
    };

    try {
      const response = await merchantApi.create(requestData);
      if (response.success && response.data) {
        submitResult = {
          success: true,
          message: `가맹점 "${response.data.name}"이(가) 성공적으로 등록되었습니다.`,
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
          err instanceof Error ? err.message : "서버와 통신할 수 없습니다.",
      };
    } finally {
      loading = false;
    }
  }

  function resetForm() {
    businessType = MerchantBusinessType.INDIVIDUAL;
    name = "";
    organizationId = "";
    businessNumber = "";
    corporateNumber = "";
    representative = "";
    address = "";
    contactName = "";
    contactEmail = "";
    contactPhone = "";
    selectedBusinessEntity = null;
    businessSearching = false;
    businessSearched = false;
    businessSearchError = null;
    blacklistResult = null;
    blacklistChecking = false;
    blacklistError = null;
    errors = {};
    submitResult = null;
    generateMerchantCode();
  }
</script>

<div class="max-w-[960px] mx-auto px-6 pb-12">
  <div class="mb-8 pb-6 border-b-2 border-foreground">
    <h1 class="text-3xl font-extrabold text-foreground tracking-tight">
      가맹점 등록
    </h1>
    <p class="text-muted-foreground text-sm mt-2">새로운 가맹점을 등록합니다</p>
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
                >새 가맹점 등록</Button
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
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                  <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                </svg>
              </span>
              사업자 유형
            </h3>
            <div class="flex gap-6">
              {#each Object.values(MerchantBusinessType) as type}
                <label class="flex items-center gap-3 cursor-pointer group">
                  <div
                    class="relative flex items-center justify-center w-5 h-5"
                  >
                    <input
                      type="radio"
                      name="businessType"
                      value={type}
                      checked={businessType === type}
                      onchange={() => handleBusinessTypeChange(type)}
                      class="appearance-none w-5 h-5 border-2 border-input rounded-full checked:border-primary checked:border-[6px] transition-all cursor-pointer"
                    />
                  </div>
                  <span
                    class="text-sm font-medium text-foreground group-hover:text-primary transition-colors"
                  >
                    {MERCHANT_BUSINESS_TYPE_LABELS[type]}
                  </span>
                </label>
              {/each}
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
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                  <polyline points="9 22 9 12 15 12 15 22" />
                </svg>
              </span>
              영업점 선택
            </h3>
            <div class="flex flex-col gap-2">
              <Label for="organizationId"
                >영업점 <span class="text-destructive">*</span></Label
              >
              <select
                id="organizationId"
                value={organizationId}
                onchange={(e) =>
                  (organizationId = (e.target as HTMLSelectElement).value)}
                disabled={loadingOrgs}
                class="h-11 px-4 pr-8 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.organizationId
                  ? 'border-destructive focus:ring-destructive/20'
                  : ''}"
              >
                <option value="">영업점을 선택하세요</option>
                {#each organizations as org}
                  <option value={org.id}>{org.name} ({org.code})</option>
                {/each}
              </select>
              {#if errors.organizationId}<span class="text-xs text-destructive"
                  >{errors.organizationId}</span
                >{/if}
            </div>
          </div>

          {#if businessType !== MerchantBusinessType.NON_BUSINESS}
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
                    <circle cx="11" cy="11" r="8" />
                    <path d="m21 21-4.35-4.35" />
                  </svg>
                </span>
                사업자 정보
              </h3>
              <div class="flex flex-col gap-2 mb-5">
                <Label for="businessNumber"
                  >사업자등록번호 <span class="text-destructive">*</span></Label
                >
                <div class="flex gap-2">
                  <input
                    id="businessNumber"
                    type="text"
                    value={businessNumber}
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
                  {#if canCheckBlacklist}
                    <Button
                      type="button"
                      variant="outline"
                      onclick={checkBlacklist}
                      disabled={blacklistChecking}
                      class="h-11 px-4 shrink-0"
                    >
                      {#if blacklistChecking}
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
                          <path
                            d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"
                          />
                        </svg>
                      {/if}
                      블랙리스트 조회
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
                      새로 입력
                    </Button>
                  {/if}
                </div>
                {#if errors.businessNumber}<span
                    class="text-xs text-destructive"
                    >{errors.businessNumber}</span
                  >{/if}
                {#if businessSearchError}<span class="text-xs text-destructive"
                    >{businessSearchError}</span
                  >{/if}
              </div>

              {#if blacklistResult}
                <div
                  class="mb-5 p-4 rounded-lg border {blacklistResult.isBlacklisted
                    ? 'border-destructive bg-destructive/5'
                    : 'border-emerald-200 bg-emerald-50/50'}"
                >
                  <div class="flex items-center gap-3">
                    {#if blacklistResult.isBlacklisted}
                      <Badge variant="destructive">블랙리스트</Badge>
                      <span class="text-sm text-destructive font-medium"
                        >{blacklistResult.reason ||
                          "블랙리스트에 등록된 사업자입니다."}</span
                      >
                    {:else}
                      <Badge variant="default" class="bg-emerald-500"
                        >정상</Badge
                      >
                      <span class="text-sm text-emerald-700 font-medium"
                        >블랙리스트에 등록되지 않은 사업자입니다.</span
                      >
                    {/if}
                  </div>
                </div>
              {/if}
              {#if blacklistError}<span
                  class="text-xs text-destructive mb-5 block"
                  >{blacklistError}</span
                >{/if}
              {#if errors.blacklist}<span
                  class="text-xs text-destructive mb-5 block"
                  >{errors.blacklist}</span
                >{/if}

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
                      >등록된 사업자가 없습니다. 직접 입력해주세요.</span
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

              {#if businessType === MerchantBusinessType.CORPORATION}
                <div class="flex flex-col gap-2 mb-5">
                  <Label for="corporateNumber"
                    >법인등록번호 <span class="text-destructive">*</span></Label
                  >
                  <input
                    id="corporateNumber"
                    type="text"
                    value={corporateNumber}
                    oninput={handleCorporateNumberInput}
                    placeholder="000000-0000000"
                    maxlength="14"
                    disabled={!!selectedBusinessEntity}
                    class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm font-mono focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.corporateNumber
                      ? 'border-destructive focus:ring-destructive/20'
                      : ''} {selectedBusinessEntity
                      ? 'bg-muted cursor-not-allowed'
                      : ''}"
                  />
                  {#if errors.corporateNumber}<span
                      class="text-xs text-destructive"
                      >{errors.corporateNumber}</span
                    >{/if}
                </div>
              {/if}
            </div>
          {/if}

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
              가맹점 정보
            </h3>
            <div class="grid grid-cols-2 gap-5 mb-5">
              <div class="flex flex-col gap-2">
                <Label for="merchantCode">가맹점 코드</Label>
                <input
                  id="merchantCode"
                  type="text"
                  value={merchantCode}
                  disabled
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-muted text-sm font-mono cursor-not-allowed"
                />
              </div>
              <div class="flex flex-col gap-2">
                <Label for="name"
                  >상호 <span class="text-destructive">*</span></Label
                >
                <input
                  id="name"
                  type="text"
                  value={name}
                  oninput={(e) => (name = (e.target as HTMLInputElement).value)}
                  placeholder="가맹점 상호를 입력하세요"
                  disabled={!!selectedBusinessEntity}
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.name
                    ? 'border-destructive focus:ring-destructive/20'
                    : ''} {selectedBusinessEntity
                    ? 'bg-muted cursor-not-allowed'
                    : ''}"
                />
                {#if errors.name}<span class="text-xs text-destructive"
                    >{errors.name}</span
                  >{/if}
              </div>
            </div>
            <div class="grid grid-cols-2 gap-5 mb-5">
              <div class="flex flex-col gap-2">
                <Label for="representative"
                  >대표자 {#if businessType === MerchantBusinessType.NON_BUSINESS}<span
                      class="text-destructive">*</span
                    >{/if}</Label
                >
                <input
                  id="representative"
                  type="text"
                  value={representative}
                  oninput={(e) =>
                    (representative = (e.target as HTMLInputElement).value)}
                  placeholder="대표자명"
                  disabled={!!selectedBusinessEntity}
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.representative
                    ? 'border-destructive focus:ring-destructive/20'
                    : ''} {selectedBusinessEntity
                    ? 'bg-muted cursor-not-allowed'
                    : ''}"
                />
                {#if errors.representative}<span
                    class="text-xs text-destructive"
                    >{errors.representative}</span
                  >{/if}
              </div>
              <div class="flex flex-col gap-2">
                <Label for="address"
                  >주소 {#if businessType === MerchantBusinessType.NON_BUSINESS}<span
                      class="text-destructive">*</span
                    >{/if}</Label
                >
                <input
                  id="address"
                  type="text"
                  value={address}
                  oninput={(e) =>
                    (address = (e.target as HTMLInputElement).value)}
                  placeholder="사업장 주소"
                  disabled={!!selectedBusinessEntity}
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.address
                    ? 'border-destructive focus:ring-destructive/20'
                    : ''} {selectedBusinessEntity
                    ? 'bg-muted cursor-not-allowed'
                    : ''}"
                />
                {#if errors.address}<span class="text-xs text-destructive"
                    >{errors.address}</span
                  >{/if}
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
                  <path
                    d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"
                  />
                </svg>
              </span>
              담당자 정보
            </h3>
            <div class="grid grid-cols-3 gap-5">
              <div class="flex flex-col gap-2">
                <Label for="contactName">담당자명</Label>
                <input
                  id="contactName"
                  type="text"
                  value={contactName}
                  oninput={(e) =>
                    (contactName = (e.target as HTMLInputElement).value)}
                  placeholder="담당자명"
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all"
                />
              </div>
              <div class="flex flex-col gap-2">
                <Label for="contactPhone">연락처</Label>
                <input
                  id="contactPhone"
                  type="tel"
                  value={contactPhone}
                  oninput={(e) =>
                    (contactPhone = (e.target as HTMLInputElement).value)}
                  placeholder="010-0000-0000"
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all"
                />
              </div>
              <div class="flex flex-col gap-2">
                <Label for="contactEmail">이메일</Label>
                <input
                  id="contactEmail"
                  type="email"
                  value={contactEmail}
                  oninput={(e) =>
                    (contactEmail = (e.target as HTMLInputElement).value)}
                  placeholder="example@email.com"
                  class="h-11 px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.contactEmail
                    ? 'border-destructive focus:ring-destructive/20'
                    : ''}"
                />
                {#if errors.contactEmail}<span class="text-xs text-destructive"
                    >{errors.contactEmail}</span
                  >{/if}
              </div>
            </div>
          </div>
        </div>
      </CardContent>

      <div
        class="flex justify-end items-center gap-4 px-8 py-6 bg-gradient-to-b from-muted/50 to-muted border-t border-border"
      >
        <Button variant="outline" onclick={resetForm} disabled={loading}>
          취소
        </Button>
        <Button
          onclick={handleSubmit}
          disabled={loading || isBlacklisted}
          class="bg-primary hover:bg-primary/90"
        >
          {#if loading}
            <span
              class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"
            ></span>
          {/if}
          등록
        </Button>
      </div>
    {/if}
  </Card>
</div>
