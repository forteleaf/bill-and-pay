<script lang="ts">
  import { onMount } from "svelte";
  import { toast } from "svelte-sonner";
  import { apiClient } from "../lib/api";
  import { settlementApi } from "../lib/settlementApi";
  import { tenantStore } from "../lib/stores";
  import { format } from "date-fns";
  import type { Settlement, PagedResponse } from "../types/api";
  import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
  } from "$lib/components/ui/card";
  import { Button } from "$lib/components/ui/button";
  import { Badge } from "$lib/components/ui/badge";
  import { Label } from "$lib/components/ui/label";
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
  } from "$lib/components/ui/table";
  import { Skeleton } from "$lib/components/ui/skeleton";
  import {
    Alert,
    AlertTitle,
    AlertDescription,
  } from "$lib/components/ui/alert";
  import * as Select from "$lib/components/ui/select";
  import ConfirmModal from "../components/ConfirmModal.svelte";

  let settlements = $state<Settlement[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  let pendingCount = $state(0);
  let approvedCount = $state(0);
  let paidCount = $state(0);

  let statusFilter = $state<string>("ALL");
  let currentPage = $state(0);
  let pageSize = $state(20);
  let totalCount = $state(0);
  let totalPages = $state(0);

  let sortField = $state<string>("created_at");
  let sortDirection = $state<"asc" | "desc">("desc");

  let showResettleModal = $state(false);
  let resettleTargetEventId = $state<string | null>(null);
  let resettling = $state(false);

  const displaySettlements = $derived(settlements);

  const STATUS_OPTIONS: { value: string; label: string }[] = [
    { value: "ALL", label: "전체" },
    { value: "PENDING", label: "대기" },
    { value: "PENDING_REVIEW", label: "검토 대기" },
    { value: "PROCESSING", label: "처리중" },
    { value: "COMPLETED", label: "완료" },
    { value: "FAILED", label: "실패" },
    { value: "CANCELLED", label: "취소" },
  ];

  const statusFilterLabel = $derived(
    STATUS_OPTIONS.find((o) => o.value === statusFilter)?.label || "전체",
  );

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat("ko-KR", {
      style: "currency",
      currency: "KRW",
    }).format(amount);
  }

  function getStatusVariant(
    status: string,
  ): "default" | "secondary" | "destructive" | "outline" {
    const variants: Record<
      string,
      "default" | "secondary" | "destructive" | "outline"
    > = {
      PENDING: "secondary",
      PENDING_REVIEW: "outline",
      PROCESSING: "secondary",
      COMPLETED: "default",
      FAILED: "destructive",
      CANCELLED: "outline",
    };
    return variants[status] || "outline";
  }

  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: "대기",
      PENDING_REVIEW: "검토 대기",
      PROCESSING: "처리중",
      COMPLETED: "완료",
      FAILED: "실패",
      CANCELLED: "취소",
    };
    return labels[status] || status;
  }

  function getEntryTypeLabel(entryType: string): string {
    return entryType === "CREDIT" ? "입금" : "출금";
  }

  function isResettleable(status: string): boolean {
    return status === "FAILED" || status === "PENDING_REVIEW";
  }

  function sortBy(field: string) {
    if (sortField === field) {
      sortDirection = sortDirection === "asc" ? "desc" : "asc";
    } else {
      sortField = field;
      sortDirection = "asc";
    }
    loadSettlements();
  }

  async function loadSettlements() {
    if (!tenantStore.current) {
      return;
    }

    apiClient.setTenantId(tenantStore.current);
    loading = true;
    error = null;

    try {
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString(),
        sortBy: sortField,
        direction: sortDirection.toUpperCase(),
      });

      if (statusFilter !== "ALL") {
        params.append("status", statusFilter);
      }

      const response = await apiClient.get<PagedResponse<Settlement>>(
        `/settlements?${params}`,
      );

      if (response.success && response.data) {
        settlements = response.data.content;
        totalCount = response.data.totalElements;
        totalPages = response.data.totalPages;
        currentPage = response.data.page;
      }

      if (!response.success) {
        error = response.error?.message || "데이터를 불러올 수 없습니다.";
      }

      loading = false;
    } catch (err) {
      error =
        err instanceof Error ? err.message : "데이터를 불러올 수 없습니다.";
      loading = false;
      console.error("API Error:", err);
    }
  }

  async function loadSummary() {
    if (!tenantStore.current) {
      return;
    }

    apiClient.setTenantId(tenantStore.current);

    try {
      const response = await apiClient.get<any>("/settlements/summary");

      if (response.success && response.data) {
        pendingCount = response.data.pendingCount || 0;
        approvedCount = response.data.approvedCount || 0;
        paidCount = response.data.paidCount || 0;
      }
    } catch (err) {
      console.error("Failed to load summary:", err);
    }
  }

  function openResettleModal(transactionEventId: string) {
    resettleTargetEventId = transactionEventId;
    showResettleModal = true;
  }

  async function handleResettle() {
    if (!resettleTargetEventId) return;

    resettling = true;
    showResettleModal = false;

    try {
      const response = await settlementApi.resettleByEventId(
        resettleTargetEventId,
      );

      if (response.success) {
        toast.success(
          `재정산 완료: ${response.data?.length || 0}건의 새 정산이 생성되었습니다.`,
        );
        loadSettlements();
        loadSummary();
      } else {
        toast.error(response.error?.message || "재정산에 실패했습니다.");
      }
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "재정산 중 오류가 발생했습니다.",
      );
      console.error("Resettle Error:", err);
    } finally {
      resettling = false;
      resettleTargetEventId = null;
    }
  }

  onMount(() => {
    loadSettlements();
    loadSummary();
  });

  function getSortIcon(field: string): string {
    if (sortField !== field) return "";
    return sortDirection === "asc" ? " ^" : " v";
  }
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div>
    <h1 class="text-3xl font-bold text-foreground">정산 내역</h1>
    <p class="text-muted-foreground mt-1">총 {totalCount}건</p>
  </div>

  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
    <Card class="border-l-4 border-l-yellow-500">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground"
          >대기</CardTitle
        >
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">{pendingCount}</div>
      </CardContent>
    </Card>

    <Card class="border-l-4 border-l-blue-500">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground"
          >승인</CardTitle
        >
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">{approvedCount}</div>
      </CardContent>
    </Card>

    <Card class="border-l-4 border-l-green-500">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground"
          >지급 완료</CardTitle
        >
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">{paidCount}</div>
      </CardContent>
    </Card>
  </div>

  <Card>
    <CardContent class="pt-6">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="flex flex-col gap-1.5">
          <Label>상태</Label>
          <Select.Root
            type="single"
            value={statusFilter}
            onValueChange={(v) => {
              statusFilter = v || "ALL";
              loadSettlements();
            }}
          >
            <Select.Trigger class="w-[180px]">
              {statusFilterLabel}
            </Select.Trigger>
            <Select.Content>
              {#each STATUS_OPTIONS as option}
                <Select.Item value={option.value} label={option.label}>
                  {option.label}
                </Select.Item>
              {/each}
            </Select.Content>
          </Select.Root>
        </div>

        <Button onclick={() => loadSettlements()}>
          <svg
            class="w-4 h-4 mr-2"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          새로고침
        </Button>
      </div>
    </CardContent>
  </Card>

  {#if loading}
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      {#each Array(3) as _}
        <Card>
          <CardHeader class="pb-2">
            <Skeleton class="h-4 w-24" />
          </CardHeader>
          <CardContent>
            <Skeleton class="h-8 w-16" />
          </CardContent>
        </Card>
      {/each}
    </div>

    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>구분</TableHead>
            <TableHead>유형</TableHead>
            <TableHead class="text-right">금액</TableHead>
            <TableHead class="text-right">수수료</TableHead>
            <TableHead class="text-right">정산액</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>생성일시</TableHead>
            <TableHead>액션</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each Array(5) as _}
            <TableRow>
              <TableCell><Skeleton class="h-4 w-24" /></TableCell>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell class="text-right"
                ><Skeleton class="h-4 w-24 ml-auto" /></TableCell
              >
              <TableCell class="text-right"
                ><Skeleton class="h-4 w-20 ml-auto" /></TableCell
              >
              <TableCell class="text-right"
                ><Skeleton class="h-4 w-28 ml-auto" /></TableCell
              >
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell><Skeleton class="h-4 w-36" /></TableCell>
              <TableCell><Skeleton class="h-8 w-16" /></TableCell>
            </TableRow>
          {/each}
        </TableBody>
      </Table>
    </Card>
  {:else if error}
    <div class="flex justify-center py-12">
      <Alert variant="destructive" class="max-w-lg">
        <AlertTitle>오류 발생</AlertTitle>
        <AlertDescription class="flex flex-col gap-3">
          <span>{error}</span>
          <Button
            variant="outline"
            size="sm"
            onclick={loadSettlements}
            class="self-start"
          >
            다시 시도
          </Button>
        </AlertDescription>
      </Alert>
    </div>
  {:else}
    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead
              class="cursor-pointer hover:bg-muted/50"
              onclick={() => sortBy("entity_type")}
            >
              구분{getSortIcon("entity_type")}
            </TableHead>
            <TableHead
              class="cursor-pointer hover:bg-muted/50"
              onclick={() => sortBy("entry_type")}
            >
              유형{getSortIcon("entry_type")}
            </TableHead>
            <TableHead
              class="cursor-pointer hover:bg-muted/50 text-right"
              onclick={() => sortBy("amount")}
            >
              금액{getSortIcon("amount")}
            </TableHead>
            <TableHead
              class="cursor-pointer hover:bg-muted/50 text-right"
              onclick={() => sortBy("fee_amount")}
            >
              수수료{getSortIcon("fee_amount")}
            </TableHead>
            <TableHead
              class="cursor-pointer hover:bg-muted/50 text-right"
              onclick={() => sortBy("net_amount")}
            >
              정산액{getSortIcon("net_amount")}
            </TableHead>
            <TableHead
              class="cursor-pointer hover:bg-muted/50"
              onclick={() => sortBy("status")}
            >
              상태{getSortIcon("status")}
            </TableHead>
            <TableHead
              class="cursor-pointer hover:bg-muted/50"
              onclick={() => sortBy("created_at")}
            >
              생성일시{getSortIcon("created_at")}
            </TableHead>
            <TableHead>액션</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each displaySettlements as settlement}
            <TableRow>
              <TableCell>{settlement.entityType}</TableCell>
              <TableCell>
                <Badge
                  variant={settlement.entryType === "CREDIT"
                    ? "default"
                    : "destructive"}
                >
                  {getEntryTypeLabel(settlement.entryType)}
                </Badge>
              </TableCell>
              <TableCell class="text-right font-semibold"
                >{formatCurrency(settlement.amount)}</TableCell
              >
              <TableCell class="text-right"
                >{formatCurrency(settlement.feeAmount)}</TableCell
              >
              <TableCell class="text-right font-bold text-primary"
                >{formatCurrency(settlement.netAmount)}</TableCell
              >
              <TableCell>
                <Badge variant={getStatusVariant(settlement.status)}>
                  {getStatusLabel(settlement.status)}
                </Badge>
              </TableCell>
              <TableCell
                >{format(
                  new Date(settlement.createdAt),
                  "yyyy-MM-dd HH:mm:ss",
                )}</TableCell
              >
              <TableCell>
                {#if isResettleable(settlement.status)}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={resettling}
                    onclick={() =>
                      openResettleModal(settlement.transactionEventId)}
                  >
                    재정산
                  </Button>
                {/if}
              </TableCell>
            </TableRow>
          {/each}

          {#if displaySettlements.length === 0}
            <TableRow>
              <TableCell
                colspan={8}
                class="text-center py-12 text-muted-foreground"
              >
                정산 내역이 없습니다.
              </TableCell>
            </TableRow>
          {/if}
        </TableBody>
      </Table>
    </Card>

    <div class="flex justify-center items-center gap-2">
      <Button
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onclick={() => {
          currentPage = 0;
          loadSettlements();
        }}
      >
        처음
      </Button>
      <Button
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onclick={() => {
          currentPage--;
          loadSettlements();
        }}
      >
        이전
      </Button>

      <span class="px-4 font-medium text-sm">
        {currentPage + 1} / {totalPages} 페이지
      </span>

      <Button
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1}
        onclick={() => {
          currentPage++;
          loadSettlements();
        }}
      >
        다음
      </Button>
      <Button
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1}
        onclick={() => {
          currentPage = totalPages - 1;
          loadSettlements();
        }}
      >
        마지막
      </Button>
    </div>
  {/if}
</div>

<ConfirmModal
  bind:show={showResettleModal}
  title="재정산 확인"
  message="선택한 거래 이벤트의 기존 정산을 취소하고 재정산을 진행합니다. 계속하시겠습니까?"
  confirmText="재정산"
  cancelText="취소"
  type="warning"
  onConfirm={handleResettle}
  onCancel={() => {
    showResettleModal = false;
    resettleTargetEventId = null;
  }}
/>
