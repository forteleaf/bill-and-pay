<script lang="ts">
  import { Dialog as DialogPrimitive } from "bits-ui";
  import { onMount } from "svelte";
  import { merchantPgMappingApi } from "../lib/merchantPgMappingApi";
  import { pgConnectionApi } from "../lib/pgConnectionApi";
  import {
    MerchantPgMappingStatus,
    MERCHANT_PG_MAPPING_STATUS_LABELS,
    type MerchantPgMappingDto,
    type MerchantPgMappingCreateRequest,
    type MerchantPgMappingUpdateRequest,
  } from "../types/merchantPgMapping";
  import type { PgConnectionDto } from "../types/pgConnection";
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
    merchantId: string;
  }

  let { merchantId }: Props = $props();

  let mappings = $state<MerchantPgMappingDto[]>([]);
  let pgConnections = $state<PgConnectionDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  let dialogOpen = $state(false);
  let dialogMode = $state<"add" | "edit">("add");
  let editingMapping = $state<MerchantPgMappingDto | null>(null);
  let saving = $state(false);

  let formPgConnectionId = $state("");
  let formMid = $state("");
  let formTerminalId = $state("");
  let formCatId = $state("");
  let formStatus = $state<MerchantPgMappingStatus>(MerchantPgMappingStatus.ACTIVE);

  let deleteConfirmOpen = $state(false);
  let mappingToDelete = $state<MerchantPgMappingDto | null>(null);
  let deleting = $state(false);

  onMount(() => {
    if (merchantId) {
      loadData();
    }
  });

  async function loadData() {
    loading = true;
    error = null;

    try {
      const [mappingsRes, pgConnectionsRes] = await Promise.all([
        merchantPgMappingApi.getByMerchantId(merchantId),
        pgConnectionApi.getAll()
      ]);

      if (mappingsRes.success && mappingsRes.data) {
        mappings = mappingsRes.data;
      }

      if (pgConnectionsRes.success && pgConnectionsRes.data) {
        pgConnections = pgConnectionsRes.data.content || [];
      }
    } catch (err) {
      error = "데이터를 불러올 수 없습니다.";
    } finally {
      loading = false;
    }
  }

  function resetForm() {
    formPgConnectionId = "";
    formMid = "";
    formTerminalId = "";
    formCatId = "";
    formStatus = MerchantPgMappingStatus.ACTIVE;
  }

  function openAddDialog() {
    dialogMode = "add";
    editingMapping = null;
    resetForm();
    dialogOpen = true;
  }

  function openEditDialog(mapping: MerchantPgMappingDto) {
    dialogMode = "edit";
    editingMapping = mapping;
    formPgConnectionId = mapping.pgConnectionId;
    formMid = mapping.mid;
    formTerminalId = mapping.terminalId || "";
    formCatId = mapping.catId || "";
    formStatus = mapping.status;
    dialogOpen = true;
  }

  function closeDialog() {
    dialogOpen = false;
    editingMapping = null;
    resetForm();
  }

  async function handleSave() {
    if (!formPgConnectionId || !formMid) {
      return;
    }

    saving = true;
    error = null;

    try {
      if (dialogMode === "add") {
        const request: MerchantPgMappingCreateRequest = {
          merchantId,
          pgConnectionId: formPgConnectionId,
          mid: formMid,
          terminalId: formTerminalId || undefined,
          catId: formCatId || undefined,
          status: formStatus,
        };

        const response = await merchantPgMappingApi.create(request);
        if (response.success) {
          await loadData();
          closeDialog();
        } else {
          error = response.error?.message || "등록에 실패했습니다.";
        }
      } else if (editingMapping) {
        const request: MerchantPgMappingUpdateRequest = {
          pgConnectionId: formPgConnectionId,
          mid: formMid,
          terminalId: formTerminalId || undefined,
          catId: formCatId || undefined,
          status: formStatus,
        };

        const response = await merchantPgMappingApi.update(editingMapping.id, request);
        if (response.success) {
          await loadData();
          closeDialog();
        } else {
          error = response.error?.message || "수정에 실패했습니다.";
        }
      }
    } catch (err) {
      error = dialogMode === "add" ? "등록에 실패했습니다." : "수정에 실패했습니다.";
    } finally {
      saving = false;
    }
  }

  function openDeleteConfirm(mapping: MerchantPgMappingDto) {
    mappingToDelete = mapping;
    deleteConfirmOpen = true;
  }

  function closeDeleteConfirm() {
    deleteConfirmOpen = false;
    mappingToDelete = null;
  }

  async function handleDelete() {
    if (!mappingToDelete) return;

    deleting = true;
    error = null;

    try {
      const response = await merchantPgMappingApi.delete(mappingToDelete.id);
      if (response.success) {
        await loadData();
        closeDeleteConfirm();
      } else {
        error = response.error?.message || "삭제에 실패했습니다.";
      }
    } catch (err) {
      error = "삭제에 실패했습니다.";
    } finally {
      deleting = false;
    }
  }

  async function handleToggleStatus(mapping: MerchantPgMappingDto) {
    const newStatus = mapping.status === MerchantPgMappingStatus.ACTIVE 
      ? MerchantPgMappingStatus.INACTIVE 
      : MerchantPgMappingStatus.ACTIVE;

    try {
      const response = await merchantPgMappingApi.updateStatus(mapping.id, newStatus);
      if (response.success) {
        await loadData();
      } else {
        error = response.error?.message || "상태 변경에 실패했습니다.";
      }
    } catch (err) {
      error = "상태 변경에 실패했습니다.";
    }
  }

  function getStatusVariant(status: MerchantPgMappingStatus): "default" | "secondary" {
    return status === MerchantPgMappingStatus.ACTIVE ? "default" : "secondary";
  }

  function getPgName(pgConnectionId: string): string {
    const pg = pgConnections.find(p => p.id === pgConnectionId);
    return pg?.pgName || pg?.pgCode || pgConnectionId;
  }
</script>

<Card>
  <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-4">
    <CardTitle class="text-base font-semibold">PG 매핑 정보</CardTitle>
    <Button size="sm" onclick={openAddDialog}>
      <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 5v14m-7-7h14" />
      </svg>
      매핑 추가
    </Button>
  </CardHeader>
  <CardContent>
    {#if loading}
      <div class="flex flex-col items-center justify-center py-8 gap-3 text-muted-foreground">
        <div class="w-8 h-8 border-3 border-muted border-t-primary rounded-full animate-spin"></div>
        <span class="text-sm">불러오는 중...</span>
      </div>
    {:else if error}
      <div class="flex flex-col items-center justify-center py-8 gap-3 text-destructive">
        <svg class="w-10 h-10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 8v4m0 4h.01" />
        </svg>
        <span class="text-sm">{error}</span>
        <Button variant="outline" size="sm" onclick={loadData}>다시 시도</Button>
      </div>
    {:else if mappings.length === 0}
      <div class="flex flex-col items-center justify-center py-12 gap-4 text-muted-foreground bg-muted/30 rounded-lg border-2 border-dashed border-muted">
        <div class="w-14 h-14 rounded-full bg-muted flex items-center justify-center">
          <svg class="w-7 h-7 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <div class="text-center">
          <p class="font-medium text-foreground">등록된 PG 매핑이 없습니다</p>
          <p class="text-sm mt-1">PG사 연동을 위해 매핑 정보를 추가하세요</p>
        </div>
        <Button size="sm" onclick={openAddDialog}>
          <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14m-7-7h14" />
          </svg>
          첫 매핑 추가
        </Button>
      </div>
    {:else}
      <div class="space-y-3">
        {#each mappings as mapping (mapping.id)}
          <div
            class={cn(
              "group relative p-4 rounded-lg border transition-all hover:shadow-md",
              mapping.status === MerchantPgMappingStatus.ACTIVE
                ? "border-primary/50 bg-primary/5"
                : "border-border bg-muted/30"
            )}
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex items-start gap-4 min-w-0 flex-1">
                <div
                  class={cn(
                    "shrink-0 w-11 h-11 rounded-xl flex items-center justify-center text-xs font-bold",
                    mapping.status === MerchantPgMappingStatus.ACTIVE
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted text-muted-foreground"
                  )}
                >
                  {mapping.pgCode?.slice(0, 2) || "PG"}
                </div>

                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-foreground">
                      {mapping.pgName || getPgName(mapping.pgConnectionId)}
                    </span>
                    <Badge variant={getStatusVariant(mapping.status)} class="text-xs">
                      {MERCHANT_PG_MAPPING_STATUS_LABELS[mapping.status]}
                    </Badge>
                  </div>

                  <div class="mt-2 grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
                    <div class="flex items-center gap-2">
                      <span class="text-muted-foreground">MID:</span>
                      <span class="font-mono">{mapping.mid}</span>
                    </div>
                    {#if mapping.terminalId}
                      <div class="flex items-center gap-2">
                        <span class="text-muted-foreground">TID:</span>
                        <span class="font-mono">{mapping.terminalId}</span>
                      </div>
                    {/if}
                    {#if mapping.catId}
                      <div class="flex items-center gap-2">
                        <span class="text-muted-foreground">CAT ID:</span>
                        <span class="font-mono">{mapping.catId}</span>
                      </div>
                    {/if}
                  </div>
                </div>
              </div>

              <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 px-2 text-xs"
                  onclick={() => handleToggleStatus(mapping)}
                >
                  {mapping.status === MerchantPgMappingStatus.ACTIVE ? "비활성화" : "활성화"}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 w-8 p-0"
                  onclick={() => openEditDialog(mapping)}
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 w-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                  onclick={() => openDeleteConfirm(mapping)}
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18m-2 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
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
  onOpenChange={(v) => { if (!v) closeDialog(); }}
>
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
    />
    <DialogPrimitive.Content
      class="fixed left-[50%] top-[50%] z-50 w-full max-w-md translate-x-[-50%] translate-y-[-50%] rounded-xl border bg-background shadow-2xl duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95"
    >
      <div class="flex items-center justify-between px-6 py-4 border-b border-border">
        <div>
          <DialogPrimitive.Title class="text-lg font-bold text-foreground">
            {dialogMode === "add" ? "PG 매핑 추가" : "PG 매핑 수정"}
          </DialogPrimitive.Title>
          <DialogPrimitive.Description class="text-sm text-muted-foreground mt-0.5">
            {dialogMode === "add" ? "새로운 PG 매핑 정보를 입력하세요" : "매핑 정보를 수정합니다"}
          </DialogPrimitive.Description>
        </div>
        <DialogPrimitive.Close class="rounded-full p-2 hover:bg-muted transition-colors">
          <svg class="w-5 h-5 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </DialogPrimitive.Close>
      </div>

      <div class="px-6 py-5 space-y-5">
        <div class="space-y-2">
          <Label for="pg-select" class="text-sm font-medium">PG사 <span class="text-destructive">*</span></Label>
          <select
            id="pg-select"
            bind:value={formPgConnectionId}
            class="w-full h-10 px-3 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring transition-colors"
          >
            <option value="">PG사를 선택하세요</option>
            {#each pgConnections as pg}
              <option value={pg.id}>{pg.pgName || pg.pgCode}</option>
            {/each}
          </select>
        </div>

        <div class="space-y-2">
          <Label for="mid" class="text-sm font-medium">MID <span class="text-destructive">*</span></Label>
          <Input
            id="mid"
            type="text"
            placeholder="PG사 가맹점 ID"
            value={formMid}
            oninput={(e) => formMid = e.currentTarget.value}
            class="font-mono"
          />
          <p class="text-xs text-muted-foreground">PG사에서 발급받은 가맹점 식별자입니다</p>
        </div>

        <div class="space-y-2">
          <Label for="terminal-id" class="text-sm font-medium">단말기 ID (TID)</Label>
          <Input
            id="terminal-id"
            type="text"
            placeholder="단말기 식별자 (선택)"
            value={formTerminalId}
            oninput={(e) => formTerminalId = e.currentTarget.value}
            class="font-mono"
          />
        </div>

        <div class="space-y-2">
          <Label for="cat-id" class="text-sm font-medium">CAT ID</Label>
          <Input
            id="cat-id"
            type="text"
            placeholder="CAT 단말기 번호 (선택)"
            value={formCatId}
            oninput={(e) => formCatId = e.currentTarget.value}
            class="font-mono"
          />
        </div>

        <div class="space-y-2">
          <Label for="status-select" class="text-sm font-medium">상태</Label>
          <select
            id="status-select"
            bind:value={formStatus}
            class="w-full h-10 px-3 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring transition-colors"
          >
            {#each Object.values(MerchantPgMappingStatus) as status}
              <option value={status}>{MERCHANT_PG_MAPPING_STATUS_LABELS[status]}</option>
            {/each}
          </select>
        </div>
      </div>

      <div class="flex justify-end gap-3 px-6 py-4 border-t border-border bg-muted/30">
        <Button variant="outline" onclick={closeDialog} disabled={saving}>
          취소
        </Button>
        <Button onclick={handleSave} disabled={saving || !formPgConnectionId || !formMid}>
          {#if saving}
            <div class="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin mr-2"></div>
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
  onOpenChange={(v) => { if (!v) closeDeleteConfirm(); }}
>
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
    />
    <DialogPrimitive.Content
      class="fixed left-[50%] top-[50%] z-50 w-full max-w-sm translate-x-[-50%] translate-y-[-50%] rounded-xl border bg-background shadow-2xl duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95"
    >
      <div class="p-6">
        <div class="flex items-center justify-center w-12 h-12 mx-auto rounded-full bg-destructive/10 mb-4">
          <svg class="w-6 h-6 text-destructive" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18m-2 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
          </svg>
        </div>

        <DialogPrimitive.Title class="text-lg font-bold text-foreground text-center">
          PG 매핑 삭제
        </DialogPrimitive.Title>
        <DialogPrimitive.Description class="text-sm text-muted-foreground text-center mt-2">
          {#if mappingToDelete}
            <span class="font-medium text-foreground">{mappingToDelete.pgName || getPgName(mappingToDelete.pgConnectionId)}</span>
            <span class="font-mono"> ({mappingToDelete.mid})</span>
            <br />매핑을 삭제하시겠습니까?
          {/if}
        </DialogPrimitive.Description>

        <div class="flex gap-3 mt-6">
          <Button variant="outline" class="flex-1" onclick={closeDeleteConfirm} disabled={deleting}>
            취소
          </Button>
          <Button variant="destructive" class="flex-1" onclick={handleDelete} disabled={deleting}>
            {#if deleting}
              <div class="w-4 h-4 border-2 border-destructive-foreground/30 border-t-destructive-foreground rounded-full animate-spin mr-2"></div>
            {/if}
            삭제
          </Button>
        </div>
      </div>
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
</DialogPrimitive.Root>
