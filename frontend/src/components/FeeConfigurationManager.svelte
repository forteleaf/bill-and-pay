<script lang="ts">
  import { Dialog as DialogPrimitive } from "bits-ui";
  import { onMount } from "svelte";
  import { feeConfigApi } from "../lib/feeConfigApi";
  import {
    FeeType,
    FeeConfigStatus,
    FEE_TYPE_LABELS,
    FEE_CONFIG_STATUS_LABELS,
    FEE_HISTORY_ACTION_LABELS,
    FEE_HISTORY_FIELD_LABELS,
    type FeeConfigurationDto,
    type FeeConfigHistoryDto,
    type FeeConfigurationCreateRequest,
    type FeeConfigurationUpdateRequest,
    type PaymentMethodDto,
  } from "../types/feeConfiguration";
  import { Button } from "$lib/components/ui/button";
  import { Badge } from "$lib/components/ui/badge";
  import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
  } from "$lib/components/ui/card";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import * as Select from "$lib/components/ui/select";
  import { cn } from "$lib/utils";

  interface Props {
    merchantId: string;
  }

  let { merchantId }: Props = $props();

  // State
  let configs = $state<FeeConfigurationDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  // Dialog state
  let dialogOpen = $state(false);
  let dialogMode = $state<"add" | "edit">("add");
  let editingConfig = $state<FeeConfigurationDto | null>(null);
  let saving = $state(false);

  // Form state
  let formPaymentMethodId = $state("");
  let formFeeType = $state<string>(FeeType.PERCENTAGE);
  let formFeeRate = $state("");
  let formFixedFee = $state("");
  let formReason = $state("");

  // Payment methods
  let paymentMethods = $state<PaymentMethodDto[]>([]);

  // History state
  let historyOpen = $state(false);
  let historyList = $state<FeeConfigHistoryDto[]>([]);
  let historyLoading = $state(false);

  onMount(() => {
    loadPaymentMethods();
    if (merchantId) {
      loadConfigs();
    }
  });

  async function loadPaymentMethods() {
    try {
      const response = await feeConfigApi.getPaymentMethods();
      if (response.success && response.data) {
        paymentMethods = response.data;
      }
    } catch (err) {
      // silent - fallback to ID display
    }
  }

  async function loadConfigs() {
    loading = true;
    error = null;
    try {
      const response = await feeConfigApi.listByMerchant(merchantId);
      if (response.success && response.data) {
        configs = response.data;
      } else {
        error = response.error?.message || "수수료 설정을 불러올 수 없습니다.";
      }
    } catch (err) {
      error = "수수료 설정을 불러올 수 없습니다.";
    } finally {
      loading = false;
    }
  }

  function resetForm() {
    formPaymentMethodId = "";
    formFeeType = FeeType.PERCENTAGE;
    formFeeRate = "";
    formFixedFee = "";
    formReason = "";
  }

  function openAddDialog() {
    dialogMode = "add";
    editingConfig = null;
    resetForm();
    dialogOpen = true;
  }

  function openEditDialog(config: FeeConfigurationDto) {
    dialogMode = "edit";
    editingConfig = config;
    formPaymentMethodId = config.paymentMethodId;
    formFeeType = config.feeType;
    formFeeRate =
      config.feeType === FeeType.PERCENTAGE
        ? (config.feeRate * 100).toFixed(2).replace(/\.?0+$/, "")
        : "";
    formFixedFee =
      config.feeType === FeeType.FIXED && config.fixedFee != null
        ? String(config.fixedFee)
        : "";
    formReason = "";
    dialogOpen = true;
  }

  function closeDialog() {
    dialogOpen = false;
    editingConfig = null;
    resetForm();
  }

  async function handleSave() {
    if (!formPaymentMethodId) return;

    saving = true;
    error = null;

    try {
      if (dialogMode === "add") {
        const request: FeeConfigurationCreateRequest = {
          paymentMethodId: formPaymentMethodId,
          feeType: formFeeType as FeeType,
          feeRate:
            formFeeType === FeeType.PERCENTAGE
              ? parseFloat(formFeeRate) / 100
              : undefined,
          fixedFee:
            formFeeType === FeeType.FIXED
              ? parseFloat(formFixedFee)
              : undefined,
          reason: formReason || undefined,
        };

        const response = await feeConfigApi.create(merchantId, request);
        if (response.success) {
          await loadConfigs();
          closeDialog();
        } else {
          error = response.error?.message || "수수료 설정 등록에 실패했습니다.";
        }
      } else if (editingConfig) {
        const request: FeeConfigurationUpdateRequest = {
          paymentMethodId: formPaymentMethodId,
          feeType: formFeeType as FeeType,
          feeRate:
            formFeeType === FeeType.PERCENTAGE
              ? parseFloat(formFeeRate) / 100
              : undefined,
          fixedFee:
            formFeeType === FeeType.FIXED
              ? parseFloat(formFixedFee)
              : undefined,
          reason: formReason,
        };

        const response = await feeConfigApi.update(
          editingConfig.id,
          request,
        );
        if (response.success) {
          await loadConfigs();
          closeDialog();
        } else {
          error = response.error?.message || "수수료 설정 수정에 실패했습니다.";
        }
      }
    } catch (err) {
      error =
        dialogMode === "add"
          ? "수수료 설정 등록에 실패했습니다."
          : "수수료 설정 수정에 실패했습니다.";
    } finally {
      saving = false;
    }
  }

  async function handleToggleStatus(config: FeeConfigurationDto) {
    error = null;
    try {
      if (config.status === FeeConfigStatus.ACTIVE) {
        const response = await feeConfigApi.deactivate(config.id);
        if (!response.success) {
          error = response.error?.message || "비활성화에 실패했습니다.";
          return;
        }
      } else {
        const response = await feeConfigApi.activate(config.id);
        if (!response.success) {
          error = response.error?.message || "활성화에 실패했습니다.";
          return;
        }
      }
      await loadConfigs();
    } catch (err) {
      error = "상태 변경에 실패했습니다.";
    }
  }

  async function loadHistory() {
    historyLoading = true;
    try {
      const response = await feeConfigApi.getMerchantHistory(merchantId);
      if (response.success && response.data) {
        historyList = response.data;
      }
    } catch (err) {
      // silent fail for history
    } finally {
      historyLoading = false;
    }
  }

  function toggleHistory() {
    historyOpen = !historyOpen;
    if (historyOpen && historyList.length === 0) {
      loadHistory();
    }
  }

  function getStatusVariant(
    status: FeeConfigStatus,
  ): "default" | "secondary" | "destructive" | "outline" {
    switch (status) {
      case FeeConfigStatus.ACTIVE:
        return "default";
      case FeeConfigStatus.INACTIVE:
        return "secondary";
      case FeeConfigStatus.PENDING:
        return "outline";
      case FeeConfigStatus.EXPIRED:
        return "destructive";
      default:
        return "outline";
    }
  }

  function formatFeeDisplay(config: FeeConfigurationDto): string {
    if (config.feeType === FeeType.PERCENTAGE) {
      return `${(config.feeRate * 100).toFixed(2).replace(/\.?0+$/, "")}%`;
    }
    if (config.feeType === FeeType.FIXED && config.fixedFee != null) {
      return `${config.fixedFee.toLocaleString()}원`;
    }
    return "-";
  }

  function formatValidPeriod(config: FeeConfigurationDto): string {
    const from = config.validFrom
      ? config.validFrom.slice(0, 10)
      : "";
    const until = config.validUntil
      ? config.validUntil.slice(0, 10)
      : "무기한";
    return from ? `${from} ~ ${until}` : until;
  }

  function formatDateTime(dateStr: string): string {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const mi = String(d.getMinutes()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
  }

  function formatHistoryChange(entry: FeeConfigHistoryDto): string {
    if (entry.fieldName === "fee_rate" && entry.oldFeeRate != null && entry.newFeeRate != null) {
      const oldPct = (entry.oldFeeRate * 100).toFixed(2).replace(/\.?0+$/, "");
      const newPct = (entry.newFeeRate * 100).toFixed(2).replace(/\.?0+$/, "");
      return `수수료율: ${oldPct}% → ${newPct}%`;
    }
    if (entry.fieldName === "status" && entry.oldStatus && entry.newStatus) {
      return `상태: ${entry.oldStatus} → ${entry.newStatus}`;
    }
    if (entry.fieldName) {
      const label = FEE_HISTORY_FIELD_LABELS[entry.fieldName] || entry.fieldName;
      const oldVal = entry.oldValue || "-";
      const newVal = entry.newValue || "-";
      return `${label}: ${oldVal} → ${newVal}`;
    }
    return FEE_HISTORY_ACTION_LABELS[entry.action] || entry.action;
  }

  let isFormValid = $derived(
    formPaymentMethodId.trim() !== "" &&
      ((formFeeType === FeeType.PERCENTAGE &&
        formFeeRate.trim() !== "" &&
        !isNaN(parseFloat(formFeeRate))) ||
        (formFeeType === FeeType.FIXED &&
          formFixedFee.trim() !== "" &&
          !isNaN(parseFloat(formFixedFee))) ||
        formFeeType === FeeType.TIERED) &&
      (dialogMode === "add" || formReason.trim() !== ""),
  );
</script>

<Card>
  <CardHeader
    class="flex flex-row items-center justify-between space-y-0 pb-4"
  >
    <CardTitle class="text-base font-semibold">수수료 설정</CardTitle>
    <Button size="sm" onclick={openAddDialog}>
      <svg
        class="w-4 h-4 mr-1.5"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <path d="M12 5v14m-7-7h14" />
      </svg>
      수수료 추가
    </Button>
  </CardHeader>
  <CardContent>
    {#if loading}
      <div
        class="flex flex-col items-center justify-center py-8 gap-3 text-muted-foreground"
      >
        <div
          class="w-8 h-8 border-3 border-muted border-t-primary rounded-full animate-spin"
        ></div>
        <span class="text-sm">불러오는 중...</span>
      </div>
    {:else if error}
      <div
        class="flex flex-col items-center justify-center py-8 gap-3 text-destructive"
      >
        <svg
          class="w-10 h-10"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        >
          <circle cx="12" cy="12" r="10" />
          <path d="M12 8v4m0 4h.01" />
        </svg>
        <span class="text-sm">{error}</span>
        <Button variant="outline" size="sm" onclick={loadConfigs}
          >다시 시도</Button
        >
      </div>
    {:else if configs.length === 0}
      <div
        class="flex flex-col items-center justify-center py-12 gap-4 text-muted-foreground bg-muted/30 rounded-lg border-2 border-dashed border-muted"
      >
        <div
          class="w-14 h-14 rounded-full bg-muted flex items-center justify-center"
        >
          <svg
            class="w-7 h-7 text-muted-foreground"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path
              d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"
            />
          </svg>
        </div>
        <div class="text-center">
          <p class="font-medium text-foreground">
            등록된 수수료 설정이 없습니다
          </p>
          <p class="text-sm mt-1">수수료를 추가하여 정산 기준을 설정하세요</p>
        </div>
        <Button size="sm" onclick={openAddDialog}>
          <svg
            class="w-4 h-4 mr-1.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 5v14m-7-7h14" />
          </svg>
          첫 수수료 추가
        </Button>
      </div>
    {:else}
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left">
              <th class="pb-3 font-medium text-muted-foreground">결제수단</th>
              <th class="pb-3 font-medium text-muted-foreground">유형</th>
              <th class="pb-3 font-medium text-muted-foreground text-right"
                >수수료</th
              >
              <th class="pb-3 font-medium text-muted-foreground">적용기간</th>
              <th class="pb-3 font-medium text-muted-foreground">상태</th>
              <th class="pb-3 font-medium text-muted-foreground text-right"
                >관리</th
              >
            </tr>
          </thead>
          <tbody>
            {#each configs as config (config.id)}
              <tr
                class="group border-b border-border/50 last:border-b-0 hover:bg-muted/30 transition-colors"
              >
                <td class="py-3 pr-4">
                  <span class="font-medium text-foreground">
                    {config.paymentMethodName || config.paymentMethodId}
                  </span>
                </td>
                <td class="py-3 pr-4">
                  <Badge variant="outline" class="text-xs">
                    {FEE_TYPE_LABELS[config.feeType] || config.feeType}
                  </Badge>
                </td>
                <td class="py-3 pr-4 text-right font-mono font-medium">
                  {formatFeeDisplay(config)}
                </td>
                <td class="py-3 pr-4 text-muted-foreground text-xs">
                  {formatValidPeriod(config)}
                </td>
                <td class="py-3 pr-4">
                  <Badge
                    variant={getStatusVariant(config.status)}
                    class="text-xs"
                  >
                    {FEE_CONFIG_STATUS_LABELS[config.status]}
                  </Badge>
                </td>
                <td class="py-3 text-right">
                  <div
                    class="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Button
                      variant="ghost"
                      size="sm"
                      class="h-8 w-8 p-0"
                      onclick={() => openEditDialog(config)}
                    >
                      <svg
                        class="w-4 h-4"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path
                          d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"
                        />
                        <path
                          d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"
                        />
                      </svg>
                    </Button>
                    {#if config.status === FeeConfigStatus.ACTIVE}
                      <Button
                        variant="ghost"
                        size="sm"
                        class="h-8 px-2 text-xs text-muted-foreground hover:text-destructive"
                        onclick={() => handleToggleStatus(config)}
                      >
                        비활성화
                      </Button>
                    {:else if config.status === FeeConfigStatus.INACTIVE}
                      <Button
                        variant="ghost"
                        size="sm"
                        class="h-8 px-2 text-xs text-muted-foreground hover:text-primary"
                        onclick={() => handleToggleStatus(config)}
                      >
                        활성화
                      </Button>
                    {/if}
                  </div>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  </CardContent>
</Card>


{#if !loading && configs.length > 0}
  <div class="mt-4">
    <button
      type="button"
      class="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors px-1 py-2"
      onclick={toggleHistory}
    >
      <svg
        class={cn(
          "w-4 h-4 transition-transform",
          historyOpen && "rotate-180",
        )}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <path d="M6 9l6 6 6-6" />
      </svg>
      변경 이력
    </button>

    {#if historyOpen}
      <Card class="mt-2">
        <CardContent class="pt-4">
          {#if historyLoading}
            <div
              class="flex flex-col items-center justify-center py-6 gap-3 text-muted-foreground"
            >
              <div
                class="w-6 h-6 border-2 border-muted border-t-primary rounded-full animate-spin"
              ></div>
              <span class="text-sm">이력 불러오는 중...</span>
            </div>
          {:else if historyList.length === 0}
            <div class="py-6 text-center text-sm text-muted-foreground">
              변경 이력이 없습니다
            </div>
          {:else}
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-border text-left">
                    <th class="pb-2 pr-4 font-medium text-muted-foreground"
                      >일시</th
                    >
                    <th class="pb-2 pr-4 font-medium text-muted-foreground"
                      >작업</th
                    >
                    <th class="pb-2 pr-4 font-medium text-muted-foreground"
                      >변경내용</th
                    >
                    <th class="pb-2 pr-4 font-medium text-muted-foreground"
                      >사유</th
                    >
                    <th class="pb-2 font-medium text-muted-foreground"
                      >변경자</th
                    >
                  </tr>
                </thead>
                <tbody>
                  {#each historyList as entry (entry.id)}
                    <tr
                      class="border-b border-border/50 last:border-b-0"
                    >
                      <td
                        class="py-2.5 pr-4 text-muted-foreground whitespace-nowrap text-xs"
                      >
                        {formatDateTime(entry.changedAt)}
                      </td>
                      <td class="py-2.5 pr-4">
                        <Badge variant="outline" class="text-xs">
                          {FEE_HISTORY_ACTION_LABELS[entry.action] ||
                            entry.action}
                        </Badge>
                      </td>
                      <td class="py-2.5 pr-4 text-foreground">
                        {formatHistoryChange(entry)}
                      </td>
                      <td class="py-2.5 pr-4 text-muted-foreground text-xs">
                        {entry.reason || "-"}
                      </td>
                      <td class="py-2.5 text-muted-foreground text-xs">
                        {entry.changedBy}
                      </td>
                    </tr>
                  {/each}
                </tbody>
              </table>
            </div>
          {/if}
        </CardContent>
      </Card>
    {/if}
  </div>
{/if}


<DialogPrimitive.Root
  bind:open={dialogOpen}
  onOpenChange={(v) => {
    if (!v) closeDialog();
  }}
>
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
    />
    <DialogPrimitive.Content
      class="fixed left-[50%] top-[50%] z-50 w-full max-w-md translate-x-[-50%] translate-y-[-50%] rounded-xl border bg-background shadow-2xl duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%]"
    >
      <div
        class="flex items-center justify-between px-6 py-4 border-b border-border"
      >
        <div>
          <DialogPrimitive.Title class="text-lg font-bold text-foreground">
            {dialogMode === "add" ? "수수료 설정 추가" : "수수료 설정 수정"}
          </DialogPrimitive.Title>
          <DialogPrimitive.Description
            class="text-sm text-muted-foreground mt-0.5"
          >
            {dialogMode === "add"
              ? "새로운 수수료 설정 정보를 입력하세요"
              : "수수료 설정을 수정합니다"}
          </DialogPrimitive.Description>
        </div>
        <DialogPrimitive.Close
          class="rounded-full p-2 hover:bg-muted transition-colors"
        >
          <svg
            class="w-5 h-5 text-muted-foreground"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </DialogPrimitive.Close>
      </div>

      <div class="px-6 py-5 space-y-5">
        
        <div class="space-y-2">
          <Label class="text-sm font-medium"
            >결제수단 <span class="text-destructive">*</span></Label
          >
          <Select.Root type="single" bind:value={formPaymentMethodId}>
            <Select.Trigger class="w-full">
              {#if formPaymentMethodId}
                {paymentMethods.find((pm) => pm.id === formPaymentMethodId)?.name || formPaymentMethodId}
              {:else}
                <span class="text-muted-foreground">결제수단을 선택하세요</span>
              {/if}
            </Select.Trigger>
            <Select.Content class="z-[60]">
              {#each paymentMethods as pm (pm.id)}
                <Select.Item value={pm.id}>{pm.name}</Select.Item>
              {/each}
            </Select.Content>
          </Select.Root>
        </div>

        
        <div class="space-y-2">
          <Label class="text-sm font-medium"
            >수수료 유형 <span class="text-destructive">*</span></Label
          >
          <Select.Root type="single" bind:value={formFeeType}>
            <Select.Trigger class="w-full">
              {#if formFeeType === FeeType.PERCENTAGE}
                정률 (%)
              {:else if formFeeType === FeeType.FIXED}
                정액 (원)
              {:else}
                <span class="text-muted-foreground">수수료 유형 선택</span>
              {/if}
            </Select.Trigger>
            <Select.Content class="z-[60]">
              <Select.Item value={FeeType.PERCENTAGE}>정률 (%)</Select.Item>
              <Select.Item value={FeeType.FIXED}>정액 (원)</Select.Item>
            </Select.Content>
          </Select.Root>
        </div>

        
        {#if formFeeType === FeeType.PERCENTAGE}
          <div class="space-y-2">
            <Label for="fee-rate" class="text-sm font-medium"
              >수수료율 (%) <span class="text-destructive">*</span></Label
            >
            <div class="relative">
              <Input
                id="fee-rate"
                type="text"
                inputmode="numeric"
                placeholder="예: 3.25"
                value={formFeeRate}
                oninput={(e) => {
                  const val = e.currentTarget.value.replace(
                    /[^0-9.]/g,
                    "",
                  );
                  e.currentTarget.value = val;
                  formFeeRate = val;
                }}
                class="pr-8 font-mono"
              />
              <span
                class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground"
                >%</span
              >
            </div>
          </div>
        {/if}

        
        {#if formFeeType === FeeType.FIXED}
          <div class="space-y-2">
            <Label for="fixed-fee" class="text-sm font-medium"
              >고정수수료 (원) <span class="text-destructive">*</span></Label
            >
            <div class="relative">
              <Input
                id="fixed-fee"
                type="text"
                inputmode="numeric"
                placeholder="예: 500"
                value={formFixedFee}
                oninput={(e) => {
                  const val = e.currentTarget.value.replace(/[^0-9]/g, "");
                  e.currentTarget.value = val;
                  formFixedFee = val;
                }}
                class="pr-8 font-mono"
              />
              <span
                class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground"
                >원</span
              >
            </div>
          </div>
        {/if}

        
        <div class="space-y-2">
          <Label for="reason" class="text-sm font-medium">
            사유
            {#if dialogMode === "edit"}
              <span class="text-destructive">*</span>
            {/if}
          </Label>
          <Input
            id="reason"
            type="text"
            placeholder={dialogMode === "edit"
              ? "변경 사유를 입력하세요 (필수)"
              : "등록 사유 (선택)"}
            value={formReason}
            oninput={(e) => (formReason = e.currentTarget.value)}
          />
        </div>
      </div>

      <div
        class="flex justify-end gap-3 px-6 py-4 border-t border-border bg-muted/30"
      >
        <Button variant="outline" onclick={closeDialog} disabled={saving}>
          취소
        </Button>
        <Button onclick={handleSave} disabled={saving || !isFormValid}>
          {#if saving}
            <div
              class="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin mr-2"
            ></div>
          {/if}
          {dialogMode === "add" ? "추가" : "저장"}
        </Button>
      </div>
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
</DialogPrimitive.Root>
