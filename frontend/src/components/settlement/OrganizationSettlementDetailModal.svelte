<script lang="ts">
  import { fade, fly } from "svelte/transition";
  import { settlementApi } from "@/api/settlement";
  import type {
    OrganizationSettlementSummary,
    OrganizationSettlementDetail,
  } from "@/types/api";
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
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
  } from "$lib/components/ui/table";

  interface Props {
    show: boolean;
    organization: OrganizationSettlementSummary | null;
    startDate?: string;
    endDate?: string;
    onClose: () => void;
  }

  let {
    show = $bindable(false),
    organization,
    startDate,
    endDate,
    onClose,
  }: Props = $props();

  let detail = $state<OrganizationSettlementDetail | null>(null);
  let loading = $state(false);
  let error = $state<string | null>(null);

  const ENTITY_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: "총판",
    AGENCY: "대리점",
    DEALER: "딜러",
    SELLER: "셀러",
    VENDOR: "벤더",
  };

  const STATUS_LABELS: Record<string, string> = {
    PENDING: "대기",
    COMPLETED: "완료",
    FAILED: "실패",
  };

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat("ko-KR", {
      style: "currency",
      currency: "KRW",
    }).format(amount);
  }

  function formatNumber(num: number): string {
    return new Intl.NumberFormat("ko-KR").format(num);
  }

  function formatPercent(rate: number): string {
    return `${(rate * 100).toFixed(2)}%`;
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return "-";
    try {
      return format(new Date(dateStr), "yyyy-MM-dd");
    } catch {
      return "-";
    }
  }

  function formatPhone(phone?: string): string {
    if (!phone) return "-";
    const cleaned = phone.replace(/\D/g, "");
    if (cleaned.length === 11) {
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-${cleaned.slice(7)}`;
    } else if (cleaned.length === 10) {
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 6)}-${cleaned.slice(6)}`;
    }
    return phone;
  }

  function getStatusVariant(
    status?: string,
  ):
    | "default"
    | "secondary"
    | "destructive"
    | "outline"
    | "success"
    | "warning" {
    const variants: Record<
      string,
      | "default"
      | "secondary"
      | "destructive"
      | "outline"
      | "success"
      | "warning"
    > = {
      PENDING: "warning",
      COMPLETED: "success",
      FAILED: "destructive",
    };
    return variants[status || ""] || "outline";
  }

  async function loadDetail(orgId: string) {
    loading = true;
    error = null;

    try {
      const response = await settlementApi.getOrganizationSettlementDetail(
        orgId,
        startDate,
        endDate,
      );
      if (response.success && response.data) {
        detail = response.data;
      } else {
        error = response.error?.message || "상세 정보를 불러올 수 없습니다.";
      }
    } catch (err) {
      error =
        err instanceof Error ? err.message : "상세 정보를 불러올 수 없습니다.";
    } finally {
      loading = false;
    }
  }

  $effect(() => {
    if (show && organization?.organizationId) {
      loadDetail(organization.organizationId);
    } else {
      detail = null;
      error = null;
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

{#if show && organization}
  <div
    class="fixed inset-0 bg-black/50 flex items-start justify-center z-50 p-4 overflow-y-auto"
    transition:fade={{ duration: 200 }}
    onclick={handleBackdropClick}
    onkeydown={(e) =>
      e.key === "Enter" && handleBackdropClick(e as unknown as MouseEvent)}
    role="button"
    tabindex="-1"
  >
    <div
      class="w-full max-w-5xl my-8"
      transition:fly={{ y: -20, duration: 300 }}
    >
      <Card class="shadow-xl">
        <CardHeader class="border-b">
          <div class="flex items-center justify-between">
            <div>
              <CardTitle class="text-xl"
                >{organization.orgName} 정산 상세</CardTitle
              >
              <p class="text-sm text-muted-foreground mt-1">
                {organization.orgCode} · {ENTITY_TYPE_LABELS[
                  organization.orgType
                ] || organization.orgType}
              </p>
            </div>
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
          {#if loading}
            <div class="flex items-center justify-center py-16">
              <div
                class="w-8 h-8 border-2 border-muted border-t-primary rounded-full animate-spin"
              ></div>
              <span class="ml-3 text-muted-foreground">로딩 중...</span>
            </div>
          {:else if error}
            <div class="text-center py-16 text-destructive">
              <p>{error}</p>
            </div>
          {:else}
            <div
              class="grid grid-cols-2 md:grid-cols-4 gap-4 bg-muted/50 rounded-lg p-4"
            >
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">대표자</p>
                <p class="font-medium">
                  {organization.representativeName || "-"}
                </p>
              </div>
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">연락처</p>
                <p class="font-medium">{formatPhone(organization.mainPhone)}</p>
              </div>
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">가맹점 수</p>
                <p class="font-medium">
                  {formatNumber(organization.merchantCount)}개
                </p>
              </div>
              <div class="space-y-1">
                <p class="text-xs text-muted-foreground">정산 상태</p>
                <Badge variant={getStatusVariant(organization.primaryStatus)}>
                  {STATUS_LABELS[organization.primaryStatus || ""] || "-"}
                </Badge>
              </div>
            </div>

            <Separator />

            <div>
              <h3 class="text-sm font-semibold text-muted-foreground mb-3">
                거래 요약
              </h3>
              <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
                <div class="bg-green-500/10 rounded-lg p-3 text-center">
                  <p class="text-xs text-muted-foreground">승인금액</p>
                  <p class="text-lg font-bold text-green-600">
                    {formatCurrency(organization.approvalAmount)}
                  </p>
                  <p class="text-xs text-muted-foreground">
                    {formatNumber(organization.approvalCount)}건
                  </p>
                </div>
                <div class="bg-red-500/10 rounded-lg p-3 text-center">
                  <p class="text-xs text-muted-foreground">취소금액</p>
                  <p class="text-lg font-bold text-red-600">
                    {formatCurrency(organization.cancelAmount)}
                  </p>
                  <p class="text-xs text-muted-foreground">
                    {formatNumber(organization.cancelCount)}건
                  </p>
                </div>
                <div class="bg-blue-500/10 rounded-lg p-3 text-center">
                  <p class="text-xs text-muted-foreground">결제금액</p>
                  <p class="text-lg font-bold text-blue-600">
                    {formatCurrency(organization.netPaymentAmount)}
                  </p>
                  <p class="text-xs text-muted-foreground">
                    {formatNumber(organization.totalTransactionCount)}건
                  </p>
                </div>
                <div class="bg-purple-500/10 rounded-lg p-3 text-center">
                  <p class="text-xs text-muted-foreground">가맹점수수료</p>
                  <p class="text-lg font-bold text-purple-600">
                    {formatCurrency(organization.merchantFeeAmount)}
                  </p>
                </div>
                <div class="bg-indigo-500/10 rounded-lg p-3 text-center">
                  <p class="text-xs text-muted-foreground">영업점수수료</p>
                  <p class="text-lg font-bold text-indigo-600">
                    {formatCurrency(organization.orgFeeAmount)}
                  </p>
                </div>
              </div>
            </div>

            {#if detail?.hierarchyFees && detail.hierarchyFees.length > 0}
              <Separator />

              <div>
                <h3 class="text-sm font-semibold text-muted-foreground mb-3">
                  계층별 수수료
                </h3>
                <div class="border rounded-lg overflow-hidden">
                  <Table>
                    <TableHeader>
                      <TableRow class="bg-muted/50">
                        <TableHead class="font-bold text-xs uppercase"
                          >유형</TableHead
                        >
                        <TableHead class="font-bold text-xs uppercase"
                          >영업점명</TableHead
                        >
                        <TableHead class="font-bold text-xs uppercase"
                          >영업점코드</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >수수료율</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >수수료</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >정산금액</TableHead
                        >
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {#each detail.hierarchyFees as fee}
                        <TableRow>
                          <TableCell>
                            <Badge variant="outline" class="text-xs">
                              {ENTITY_TYPE_LABELS[fee.entityType] ||
                                fee.entityType}
                            </Badge>
                          </TableCell>
                          <TableCell class="font-medium"
                            >{fee.entityName}</TableCell
                          >
                          <TableCell
                            class="font-mono text-sm text-muted-foreground"
                            >{fee.entityCode}</TableCell
                          >
                          <TableCell class="text-right"
                            >{formatPercent(fee.feeRate)}</TableCell
                          >
                          <TableCell class="text-right"
                            >{formatCurrency(fee.feeAmount)}</TableCell
                          >
                          <TableCell class="text-right font-semibold"
                            >{formatCurrency(fee.netAmount)}</TableCell
                          >
                        </TableRow>
                      {/each}
                    </TableBody>
                  </Table>
                </div>
              </div>
            {/if}

            {#if detail?.calculation}
              <Separator />

              <div>
                <h3 class="text-sm font-semibold text-muted-foreground mb-3">
                  정산 계산
                </h3>
                <div
                  class="grid grid-cols-2 md:grid-cols-4 gap-4 bg-muted/30 rounded-lg p-4"
                >
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">정산수수료율</p>
                    <p class="text-lg font-semibold">
                      {formatPercent(detail.calculation.settlementFeeRate)}
                    </p>
                  </div>
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">매출액 (총액)</p>
                    <p class="text-lg font-semibold">
                      {formatCurrency(detail.calculation.grossAmount)}
                    </p>
                  </div>
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">공급가</p>
                    <p class="text-lg font-medium">
                      {formatCurrency(detail.calculation.supplyAmount)}
                    </p>
                  </div>
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">부가세</p>
                    <p class="text-lg font-medium">
                      {formatCurrency(detail.calculation.vatAmount)}
                    </p>
                  </div>
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">최종금액</p>
                    <p class="text-lg font-semibold">
                      {formatCurrency(detail.calculation.finalAmount)}
                    </p>
                  </div>
                  <div class="space-y-1">
                    <p class="text-xs text-muted-foreground">
                      하위영업점수수료
                    </p>
                    <p class="text-lg font-medium text-muted-foreground">
                      {formatCurrency(detail.calculation.childOrgFeeAmount)}
                    </p>
                  </div>
                  <div
                    class="col-span-2 space-y-1 bg-primary/10 rounded-lg p-3"
                  >
                    <p class="text-xs text-muted-foreground">가맹점 실지급액</p>
                    <p class="text-2xl font-bold text-primary">
                      {formatCurrency(detail.calculation.merchantPayoutAmount)}
                    </p>
                  </div>
                </div>
              </div>
            {/if}

            {#if detail?.merchantSettlements && detail.merchantSettlements.length > 0}
              <Separator />

              <div>
                <h3 class="text-sm font-semibold text-muted-foreground mb-3">
                  가맹점별 거래내역
                </h3>
                <div
                  class="border rounded-lg overflow-hidden max-h-[400px] overflow-y-auto"
                >
                  <Table>
                    <TableHeader class="sticky top-0 bg-background z-10">
                      <TableRow class="bg-muted/50">
                        <TableHead class="font-bold text-xs uppercase"
                          >일자</TableHead
                        >
                        <TableHead class="font-bold text-xs uppercase"
                          >영업점</TableHead
                        >
                        <TableHead class="font-bold text-xs uppercase"
                          >가맹점</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >승인금액</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >취소금액</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >결제금액</TableHead
                        >
                        <TableHead
                          class="text-right font-bold text-xs uppercase"
                          >수수료</TableHead
                        >
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {#each detail.merchantSettlements as ms}
                        <TableRow class="hover:bg-muted/30">
                          <TableCell class="text-sm"
                            >{formatDate(ms.transactionDate)}</TableCell
                          >
                          <TableCell class="text-sm">{ms.branchName}</TableCell>
                          <TableCell>
                            <div>
                              <p class="font-medium text-sm">
                                {ms.merchantName}
                              </p>
                              <p
                                class="text-xs text-muted-foreground font-mono"
                              >
                                {ms.merchantCode}
                              </p>
                            </div>
                          </TableCell>
                          <TableCell class="text-right">
                            <span class="text-green-600 font-medium"
                              >{formatCurrency(ms.approvalAmount)}</span
                            >
                            <span class="text-xs text-muted-foreground ml-1"
                              >({ms.approvalCount})</span
                            >
                          </TableCell>
                          <TableCell class="text-right">
                            <span class="text-red-600 font-medium"
                              >{formatCurrency(ms.cancelAmount)}</span
                            >
                            <span class="text-xs text-muted-foreground ml-1"
                              >({ms.cancelCount})</span
                            >
                          </TableCell>
                          <TableCell class="text-right font-semibold"
                            >{formatCurrency(ms.netPaymentAmount)}</TableCell
                          >
                          <TableCell class="text-right text-muted-foreground"
                            >{formatCurrency(ms.feeAmount)}</TableCell
                          >
                        </TableRow>
                      {/each}
                    </TableBody>
                  </Table>
                </div>
              </div>
            {/if}
          {/if}
        </CardContent>
      </Card>
    </div>
  </div>
{/if}
