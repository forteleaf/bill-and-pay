<script lang="ts">
  import {
    Dialog as DialogPrimitive,
    Popover as PopoverPrimitive,
  } from "bits-ui";
  import { onMount } from "svelte";
  import { settlementAccountApi } from "../lib/settlementAccountApi";
  import {
    KOREAN_BANK_CODES,
    SETTLEMENT_ACCOUNT_STATUS_LABELS,
    SettlementAccountStatus,
    type SettlementAccountDto,
    type SettlementAccountCreateRequest,
    type SettlementAccountUpdateRequest,
    type SettlementAccountEntityType,
  } from "../types/settlementAccount";
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
  import { cn } from "$lib/utils";

  interface Props {
    entityType: SettlementAccountEntityType;
    entityId: string;
  }

  let { entityType, entityId }: Props = $props();

  // State
  let accounts = $state<SettlementAccountDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  // Dialog state
  let dialogOpen = $state(false);
  let dialogMode = $state<"add" | "edit">("add");
  let editingAccount = $state<SettlementAccountDto | null>(null);
  let saving = $state(false);

  // Form state
  let formBankCode = $state("");
  let formAccountNumber = $state("");
  let formAccountHolder = $state("");
  let formMemo = $state("");
  let formIsPrimary = $state(false);

  // Delete confirmation state
  let deleteConfirmOpen = $state(false);
  let accountToDelete = $state<SettlementAccountDto | null>(null);
  let deleting = $state(false);

  // Bank combobox state
  let bankSearch = $state("");
  let bankPopoverOpen = $state(false);

  // Sorted bank codes for dropdown
  const sortedBankCodes = Object.entries(KOREAN_BANK_CODES).sort((a, b) =>
    a[1].localeCompare(b[1], "ko"),
  );

  // Filtered bank codes based on search
  const filteredBankCodes = $derived(
    bankSearch.trim()
      ? sortedBankCodes.filter(([_, name]) =>
          name.toLowerCase().includes(bankSearch.trim().toLowerCase()),
        )
      : sortedBankCodes,
  );

  onMount(() => {
    if (entityId) {
      loadAccounts();
    }
  });

  async function loadAccounts() {
    loading = true;
    error = null;

    try {
      const response = await settlementAccountApi.listByEntity(
        entityType,
        entityId,
      );
      if (response.success && response.data) {
        accounts = response.data.content || [];
      } else {
        error =
          response.error?.message || "정산계좌 목록을 불러올 수 없습니다.";
      }
    } catch (err) {
      error = "정산계좌 목록을 불러올 수 없습니다.";
    } finally {
      loading = false;
    }
  }

  function resetForm() {
    formBankCode = "";
    formAccountNumber = "";
    formAccountHolder = "";
    formMemo = "";
    formIsPrimary = false;
  }

  function openAddDialog() {
    dialogMode = "add";
    editingAccount = null;
    resetForm();
    dialogOpen = true;
  }

  function openEditDialog(account: SettlementAccountDto) {
    dialogMode = "edit";
    editingAccount = account;
    formBankCode = account.bankCode;
    formAccountNumber = account.accountNumber;
    formAccountHolder = account.accountHolder;
    formMemo = account.memo || "";
    formIsPrimary = account.isPrimary;
    dialogOpen = true;
  }

  function closeDialog() {
    dialogOpen = false;
    editingAccount = null;
    resetForm();
  }

  async function handleSave() {
    if (!formBankCode || !formAccountNumber || !formAccountHolder) {
      return;
    }

    saving = true;
    error = null;

    try {
      if (dialogMode === "add") {
        const request: SettlementAccountCreateRequest = {
          bankCode: formBankCode,
          accountNumber: formAccountNumber,
          accountHolder: formAccountHolder,
          memo: formMemo || undefined,
          entityType,
          entityId,
        };

        const response = await settlementAccountApi.create(request);
        if (response.success && response.data) {
          if (formIsPrimary) {
            await settlementAccountApi.setPrimary(response.data.id);
          }
          await loadAccounts();
          closeDialog();
        } else {
          error = response.error?.message || "계좌 등록에 실패했습니다.";
        }
      } else if (editingAccount) {
        const request: SettlementAccountUpdateRequest = {
          bankCode: formBankCode,
          accountNumber: formAccountNumber,
          accountHolder: formAccountHolder,
          memo: formMemo || undefined,
        };

        const response = await settlementAccountApi.update(
          editingAccount.id,
          request,
        );
        if (response.success) {
          if (formIsPrimary && !editingAccount.isPrimary) {
            await settlementAccountApi.setPrimary(editingAccount.id);
          }
          await loadAccounts();
          closeDialog();
        } else {
          error = response.error?.message || "계좌 수정에 실패했습니다.";
        }
      }
    } catch (err) {
      error =
        dialogMode === "add"
          ? "계좌 등록에 실패했습니다."
          : "계좌 수정에 실패했습니다.";
    } finally {
      saving = false;
    }
  }

  function openDeleteConfirm(account: SettlementAccountDto) {
    accountToDelete = account;
    deleteConfirmOpen = true;
  }

  function closeDeleteConfirm() {
    deleteConfirmOpen = false;
    accountToDelete = null;
  }

  async function handleDelete() {
    if (!accountToDelete) return;

    deleting = true;
    error = null;

    try {
      const response = await settlementAccountApi.delete(accountToDelete.id);
      if (response.success) {
        await loadAccounts();
        closeDeleteConfirm();
      } else {
        error = response.error?.message || "계좌 삭제에 실패했습니다.";
      }
    } catch (err) {
      error = "계좌 삭제에 실패했습니다.";
    } finally {
      deleting = false;
    }
  }

  async function handleSetPrimary(account: SettlementAccountDto) {
    if (account.isPrimary) return;

    error = null;

    try {
      const response = await settlementAccountApi.setPrimary(account.id);
      if (response.success) {
        await loadAccounts();
      } else {
        error = response.error?.message || "주계좌 설정에 실패했습니다.";
      }
    } catch (err) {
      error = "주계좌 설정에 실패했습니다.";
    }
  }

  function getStatusVariant(
    status: SettlementAccountStatus,
  ): "default" | "secondary" | "destructive" | "outline" {
    switch (status) {
      case SettlementAccountStatus.ACTIVE:
        return "default";
      case SettlementAccountStatus.INACTIVE:
        return "secondary";
      case SettlementAccountStatus.PENDING_VERIFICATION:
        return "outline";
      default:
        return "outline";
    }
  }
</script>

<Card>
  <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-4">
    <CardTitle class="text-base font-semibold">정산계좌</CardTitle>
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
      계좌 추가
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
        <Button variant="outline" size="sm" onclick={loadAccounts}
          >다시 시도</Button
        >
      </div>
    {:else if accounts.length === 0}
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
            <rect x="1" y="4" width="22" height="16" rx="2" />
            <path d="M1 10h22" />
          </svg>
        </div>
        <div class="text-center">
          <p class="font-medium text-foreground">등록된 정산계좌가 없습니다</p>
          <p class="text-sm mt-1">계좌를 추가하여 정산을 받으세요</p>
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
          첫 계좌 추가
        </Button>
      </div>
    {:else}
      <div class="space-y-3">
        {#each accounts as account (account.id)}
          <div
            class={cn(
              "group relative p-4 rounded-lg border transition-all hover:shadow-md",
              account.isPrimary
                ? "border-primary/50 bg-primary/5 ring-1 ring-primary/20"
                : "border-border bg-background hover:border-muted-foreground/30",
            )}
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex items-start gap-4 min-w-0 flex-1">
                <div
                  class={cn(
                    "shrink-0 w-11 h-11 rounded-xl flex items-center justify-center text-sm font-bold",
                    account.isPrimary
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted text-muted-foreground",
                  )}
                >
                  {KOREAN_BANK_CODES[account.bankCode]?.slice(0, 2) || "??"}
                </div>

                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-foreground">
                      {account.bankName ||
                        KOREAN_BANK_CODES[account.bankCode] ||
                        account.bankCode}
                    </span>
                    {#if account.isPrimary}
                      <Badge variant="default" class="text-xs px-2 py-0.5">
                        <svg
                          class="w-3 h-3 mr-1"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                        >
                          <path
                            d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                          />
                        </svg>
                        주계좌
                      </Badge>
                    {/if}
                    <Badge
                      variant={getStatusVariant(account.status)}
                      class="text-xs"
                    >
                      {SETTLEMENT_ACCOUNT_STATUS_LABELS[account.status]}
                    </Badge>
                  </div>

                  <div class="mt-1.5 flex items-center gap-3 text-sm">
                    <span class="font-mono text-muted-foreground tracking-wide">
                      {account.maskedAccountNumber || account.accountNumber}
                    </span>
                    <span class="text-muted-foreground/50">|</span>
                    <span class="text-foreground">{account.accountHolder}</span>
                  </div>

                  {#if account.memo}
                    <p
                      class="mt-2 text-xs text-muted-foreground bg-muted/50 rounded px-2 py-1 inline-block"
                    >
                      {account.memo}
                    </p>
                  {/if}
                </div>
              </div>

              <div
                class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                {#if !account.isPrimary}
                  <Button
                    variant="ghost"
                    size="sm"
                    class="h-8 px-2 text-xs"
                    onclick={() => handleSetPrimary(account)}
                  >
                    <svg
                      class="w-3.5 h-3.5 mr-1"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path
                        d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                      />
                    </svg>
                    주계좌
                  </Button>
                {/if}
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 w-8 p-0"
                  onclick={() => openEditDialog(account)}
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
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 w-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                  onclick={() => openDeleteConfirm(account)}
                >
                  <svg
                    class="w-4 h-4"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      d="M3 6h18m-2 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
                    />
                    <line x1="10" y1="11" x2="10" y2="17" />
                    <line x1="14" y1="11" x2="14" y2="17" />
                  </svg>
                </Button>
              </div>
            </div>
          </div>
        {/each}
      </div>
    {/if}
  </CardContent>
</Card>

<!-- Add/Edit Dialog -->
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
            {dialogMode === "add" ? "정산계좌 추가" : "정산계좌 수정"}
          </DialogPrimitive.Title>
          <DialogPrimitive.Description
            class="text-sm text-muted-foreground mt-0.5"
          >
            {dialogMode === "add"
              ? "새로운 정산계좌 정보를 입력하세요"
              : "계좌 정보를 수정합니다"}
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
        <!-- Bank Selection -->
        <div class="space-y-2">
          <Label for="bank-select" class="text-sm font-medium"
            >은행 <span class="text-destructive">*</span></Label
          >
          <PopoverPrimitive.Root bind:open={bankPopoverOpen}>
            <PopoverPrimitive.Trigger>
              <button
                id="bank-select"
                type="button"
                class={cn(
                  "flex h-9 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs transition-[color,box-shadow] outline-none select-none disabled:cursor-not-allowed disabled:opacity-50",
                  "focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]",
                  "dark:bg-input/30 dark:hover:bg-input/50",
                  !formBankCode && "text-muted-foreground",
                )}
              >
                <span class="truncate">
                  {formBankCode
                    ? KOREAN_BANK_CODES[formBankCode]
                    : "은행을 선택하세요"}
                </span>
                <svg
                  class="h-4 w-4 shrink-0 opacity-50"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="m7 15 5 5 5-5" />
                  <path d="m7 9 5-5 5 5" />
                </svg>
              </button>
            </PopoverPrimitive.Trigger>
            <PopoverPrimitive.Content
              class="z-50 w-[--bits-popover-anchor-width] rounded-lg border border-border bg-popover p-0 text-popover-foreground shadow-lg outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95"
              align="start"
              sideOffset={4}
            >
              <div class="p-2 border-b border-border">
                <Input
                  type="text"
                  placeholder="은행 검색..."
                  value={bankSearch}
                  oninput={(e) => (bankSearch = e.currentTarget.value)}
                  class="h-9"
                  autofocus
                />
              </div>
              <div class="max-h-[200px] overflow-y-auto p-1">
                {#if filteredBankCodes.length === 0}
                  <div class="py-6 text-center text-sm text-muted-foreground">
                    검색 결과가 없습니다
                  </div>
                {:else}
                  {#each filteredBankCodes as [code, name]}
                    <button
                      type="button"
                      class={cn(
                        "flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm cursor-pointer transition-colors hover:bg-muted",
                        formBankCode === code &&
                          "bg-primary/10 text-primary font-medium",
                      )}
                      onclick={() => {
                        formBankCode = code;
                        bankSearch = "";
                        bankPopoverOpen = false;
                      }}
                    >
                      <svg
                        class={cn(
                          "h-4 w-4 shrink-0",
                          formBankCode === code ? "opacity-100" : "opacity-0",
                        )}
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                      <span>{name}</span>
                    </button>
                  {/each}
                {/if}
              </div>
            </PopoverPrimitive.Content>
          </PopoverPrimitive.Root>
        </div>

        <!-- Account Number -->
        <div class="space-y-2">
          <Label for="account-number" class="text-sm font-medium"
            >계좌번호 <span class="text-destructive">*</span></Label
          >
          <Input
            id="account-number"
            type="text"
            inputmode="numeric"
            maxlength={20}
            placeholder="'-' 없이 숫자만 입력"
            value={formAccountNumber}
            oninput={(e) => {
              const numericValue = e.currentTarget.value
                .replace(/\D/g, "")
                .slice(0, 20);
              e.currentTarget.value = numericValue;
              formAccountNumber = numericValue;
            }}
            class="font-mono"
          />
        </div>

        <!-- Account Holder -->
        <div class="space-y-2">
          <Label for="account-holder" class="text-sm font-medium"
            >예금주 <span class="text-destructive">*</span></Label
          >
          <Input
            id="account-holder"
            type="text"
            placeholder="예금주명을 입력하세요"
            value={formAccountHolder}
            oninput={(e) => (formAccountHolder = e.currentTarget.value)}
          />
        </div>

        <!-- Memo -->
        <div class="space-y-2">
          <Label for="memo" class="text-sm font-medium">메모</Label>
          <Input
            id="memo"
            type="text"
            placeholder="계좌 용도 등 메모 (선택)"
            value={formMemo}
            oninput={(e) => (formMemo = e.currentTarget.value)}
          />
        </div>

        <!-- Set as Primary -->
        <label
          class="flex items-center gap-3 p-3 rounded-lg border border-border hover:bg-muted/50 cursor-pointer transition-colors select-none"
        >
          <input
            type="checkbox"
            bind:checked={formIsPrimary}
            class="w-4 h-4 rounded border-input text-primary focus:ring-primary focus:ring-offset-0"
          />
          <div>
            <span class="font-medium text-sm text-foreground"
              >주계좌로 설정</span
            >
            <p class="text-xs text-muted-foreground mt-0.5">
              정산금이 이 계좌로 입금됩니다
            </p>
          </div>
        </label>
      </div>

      <div
        class="flex justify-end gap-3 px-6 py-4 border-t border-border bg-muted/30"
      >
        <Button variant="outline" onclick={closeDialog} disabled={saving}>
          취소
        </Button>
        <Button
          onclick={handleSave}
          disabled={saving ||
            !formBankCode ||
            !formAccountNumber ||
            !formAccountHolder}
        >
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

<!-- Delete Confirmation Dialog -->
<DialogPrimitive.Root
  bind:open={deleteConfirmOpen}
  onOpenChange={(v) => {
    if (!v) closeDeleteConfirm();
  }}
>
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
    />
    <DialogPrimitive.Content
      class="fixed left-[50%] top-[50%] z-50 w-full max-w-sm translate-x-[-50%] translate-y-[-50%] rounded-xl border bg-background shadow-2xl duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95"
    >
      <div class="p-6">
        <div
          class="flex items-center justify-center w-12 h-12 mx-auto rounded-full bg-destructive/10 mb-4"
        >
          <svg
            class="w-6 h-6 text-destructive"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              d="M3 6h18m-2 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
            />
          </svg>
        </div>

        <DialogPrimitive.Title
          class="text-lg font-bold text-foreground text-center"
        >
          계좌 삭제
        </DialogPrimitive.Title>
        <DialogPrimitive.Description
          class="text-sm text-muted-foreground text-center mt-2"
        >
          {#if accountToDelete}
            <span class="font-medium text-foreground"
              >{accountToDelete.bankName ||
                KOREAN_BANK_CODES[accountToDelete.bankCode]}</span
            >
            <span class="font-mono">
              ({accountToDelete.maskedAccountNumber})</span
            >
            <br />계좌를 삭제하시겠습니까?
          {/if}
        </DialogPrimitive.Description>

        <div class="flex gap-3 mt-6">
          <Button
            variant="outline"
            class="flex-1"
            onclick={closeDeleteConfirm}
            disabled={deleting}
          >
            취소
          </Button>
          <Button
            variant="destructive"
            class="flex-1"
            onclick={handleDelete}
            disabled={deleting}
          >
            {#if deleting}
              <div
                class="w-4 h-4 border-2 border-destructive-foreground/30 border-t-destructive-foreground rounded-full animate-spin mr-2"
              ></div>
            {/if}
            삭제
          </Button>
        </div>
      </div>
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
</DialogPrimitive.Root>
