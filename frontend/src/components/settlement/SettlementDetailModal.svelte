<script lang="ts">
  import { fade, fly } from "svelte/transition";
  import { transactionApi } from "@/api/transaction";
  import type { Settlement, Transaction } from "@/types/api";
  import { format } from "date-fns";
  import { Button } from "$lib/components/ui/button";
  import { Badge } from "$lib/components/ui/badge";
  import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
  } from "$lib/components/ui/card";
  import { Separator } from "$lib/components/ui/separator";

  interface Props {
    show: boolean;
    settlement: Settlement | null;
    onClose: () => void;
  }

  let { show = $bindable(false), settlement, onClose }: Props = $props();

  let transaction = $state<Transaction | null>(null);
  let loadingTransaction = $state(false);
  let transactionError = $state<string | null>(null);

  const ENTITY_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: "총판",
    AGENCY: "대리점",
    DEALER: "딜러",
    SELLER: "셀러",
    VENDOR: "벤더",
  };

  const STATUS_LABELS: Record<string, string> = {
    PENDING: "대기",
    PROCESSING: "처리중",
    COMPLETED: "완료",
    FAILED: "실패",
    CANCELLED: "취소",
  };

  const ENTRY_TYPE_LABELS: Record<string, string> = {
    CREDIT: "입금",
    DEBIT: "출금",
  };

  const TX_STATUS_LABELS: Record<string, string> = {
    APPROVED: "승인",
    CANCELLED: "취소",
    PARTIAL_CANCELLED: "부분취소",
    PENDING: "대기",
    FAILED: "실패",
  };

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat("ko-KR", {
      style: "currency",
      currency: "KRW",
    }).format(amount);
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return "-";
    try {
      return format(new Date(dateStr), "yyyy-MM-dd HH:mm:ss");
    } catch {
      return "-";
    }
  }

  function formatPercent(rate: number): string {
    return `${(rate * 100).toFixed(2)}%`;
  }

  function getStatusVariant(
    status: string,
  ): "default" | "secondary" | "destructive" | "outline" {
    const variants: Record<
      string,
      "default" | "secondary" | "destructive" | "outline"
    > = {
      PENDING: "secondary",
      PROCESSING: "secondary",
      COMPLETED: "default",
      FAILED: "destructive",
      CANCELLED: "outline",
      APPROVED: "default",
      PARTIAL_CANCELLED: "secondary",
    };
    return variants[status] || "outline";
  }

  function getEntryTypeVariant(entryType: string): "default" | "destructive" {
    return entryType === "CREDIT" ? "default" : "destructive";
  }

  async function loadTransaction(transactionId: string) {
    loadingTransaction = true;
    transactionError = null;

    try {
      const response = await transactionApi.getById(transactionId);
      if (response.success && response.data) {
        transaction = response.data;
      } else {
        transactionError =
          response.error?.message || "원거래 정보를 불러올 수 없습니다.";
      }
    } catch (err) {
      transactionError =
        err instanceof Error
          ? err.message
          : "원거래 정보를 불러올 수 없습니다.";
    } finally {
      loadingTransaction = false;
    }
  }

  $effect(() => {
    if (show && settlement?.transactionId) {
      loadTransaction(settlement.transactionId);
    } else {
      transaction = null;
      transactionError = null;
    }
  });

  function handleBackdropClick(e: MouseEvent) {
    if (e.target === e.currentTarget) {
      onClose();
    }
  }

  function handleEscape(e: KeyboardEvent) {
    if (e.key === "Escape" && show) {
      onClose();
    }
  }
</script>

<svelte:window onkeydown={handleEscape} />

{#if show && settlement}
  <div
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto"
    transition:fade={{ duration: 200 }}
    onclick={handleBackdropClick}
    onkeydown={(e) => e.key === "Enter" && handleBackdropClick}
    role="button"
    tabindex="-1"
  >
    <div
      class="w-full max-w-3xl my-8"
      transition:fly={{ y: -20, duration: 300 }}
    >
      <Card class="shadow-xl">
        <CardHeader class="border-b">
          <div class="flex items-center justify-between">
            <CardTitle class="text-xl">정산 상세</CardTitle>
            <Button variant="ghost" size="sm" onclick={onClose}>
              <svg
                class="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </Button>
          </div>
        </CardHeader>

        <CardContent class="p-6 space-y-6">
          <div>
            <h3
              class="text-sm font-semibold text-muted-foreground mb-3 flex items-center gap-2"
            >
              <svg
                class="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"
                />
              </svg>
              정산 정보
            </h3>

            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">영업점 유형</p>
                <Badge variant="outline" class="font-medium">
                  {ENTITY_TYPE_LABELS[settlement.entityType] ||
                    settlement.entityType}
                </Badge>
              </div>

              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">구분</p>
                <Badge variant={getEntryTypeVariant(settlement.entryType)}>
                  {ENTRY_TYPE_LABELS[settlement.entryType] ||
                    settlement.entryType}
                </Badge>
              </div>

              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">영업점 경로</p>
                <p class="font-mono text-sm">{settlement.entityPath}</p>
              </div>

              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">상태</p>
                <Badge variant={getStatusVariant(settlement.status)}>
                  {STATUS_LABELS[settlement.status] || settlement.status}
                </Badge>
              </div>
            </div>

            <Separator class="my-4" />

            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">거래금액</p>
                <p class="text-lg font-semibold">
                  {formatCurrency(settlement.amount)}
                </p>
              </div>

              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">수수료율</p>
                <p class="text-lg font-medium text-muted-foreground">
                  {formatPercent(settlement.feeRate)}
                </p>
              </div>

              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">수수료</p>
                <p class="text-lg font-medium text-muted-foreground">
                  {formatCurrency(settlement.feeAmount)}
                </p>
              </div>

              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">정산금액</p>
                <p class="text-lg font-bold text-primary">
                  {formatCurrency(settlement.netAmount)}
                </p>
              </div>
            </div>

            <Separator class="my-4" />

            <div class="grid grid-cols-2 gap-4 text-sm">
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">생성일시</p>
                <p>{formatDate(settlement.createdAt)}</p>
              </div>

              {#if settlement.settledAt}
                <div class="space-y-1">
                  <p class="text-xs text-muted-foreground">정산완료일시</p>
                  <p>{formatDate(settlement.settledAt)}</p>
                </div>
              {/if}
            </div>
          </div>

          <Separator />

          <div>
            <h3
              class="text-sm font-semibold text-muted-foreground mb-3 flex items-center gap-2"
            >
              <svg
                class="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
                />
              </svg>
              원거래 정보
            </h3>

            {#if loadingTransaction}
              <div class="flex items-center justify-center py-8">
                <div
                  class="w-6 h-6 border-2 border-muted border-t-primary rounded-full animate-spin"
                ></div>
                <span class="ml-2 text-muted-foreground">로딩 중...</span>
              </div>
            {:else if transactionError}
              <div class="text-center py-8 text-destructive">
                <p>{transactionError}</p>
              </div>
            {:else if transaction}
              <div class="bg-muted/50 rounded-lg p-4 space-y-4">
                <div class="grid grid-cols-2 gap-4">
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">거래번호</p>
                    <p class="font-mono text-sm font-medium">
                      {transaction.transactionId}
                    </p>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">거래상태</p>
                    <Badge variant={getStatusVariant(transaction.status)}>
                      {TX_STATUS_LABELS[transaction.status] ||
                        transaction.status}
                    </Badge>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">가맹점</p>
                    <p class="font-medium">{transaction.merchantName || "-"}</p>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">거래금액</p>
                    <p class="font-semibold">
                      {formatCurrency(transaction.amount)}
                    </p>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">승인번호</p>
                    <p class="font-mono">{transaction.approvalNumber || "-"}</p>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">PG거래ID</p>
                    <p class="font-mono text-xs">
                      {transaction.pgTransactionId}
                    </p>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">단말기ID</p>
                    <p class="font-mono">{transaction.catId || "-"}</p>
                  </div>

                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">승인일시</p>
                    <p>{formatDate(transaction.approvedAt)}</p>
                  </div>

                  {#if transaction.cancelledAt}
                    <div class="space-y-1">
                      <p class="text-xs text-muted-foreground">취소일시</p>
                      <p>{formatDate(transaction.cancelledAt)}</p>
                    </div>
                  {/if}
                </div>
              </div>
            {:else}
              <div class="text-center py-8 text-muted-foreground">
                <p>원거래 정보가 없습니다.</p>
              </div>
            {/if}
          </div>
        </CardContent>
      </Card>
    </div>
  </div>
{/if}
