<script lang="ts">
  import { Dialog as DialogPrimitive } from "bits-ui";
  import { contactApi, type ContactCreateRequest, type ContactUpdateRequest } from '../lib/contactApi';
  import { ContactRole, type ContactDto } from '../types/merchant';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { cn } from '$lib/utils';
  import * as Select from '$lib/components/ui/select';

  interface Props {
    entityType: string;
    entityId: string;
  }

  let { entityType, entityId }: Props = $props();

  const CONTACT_ROLE_LABELS: Record<ContactRole, string> = {
    [ContactRole.PRIMARY]: '대표',
    [ContactRole.SECONDARY]: '담당자',
    [ContactRole.SETTLEMENT]: '정산담당',
    [ContactRole.TECHNICAL]: '기술담당',
  };

  const ROLE_OPTIONS: { value: ContactRole; label: string }[] = [
    { value: ContactRole.PRIMARY, label: '대표' },
    { value: ContactRole.SECONDARY, label: '담당자' },
    { value: ContactRole.SETTLEMENT, label: '정산담당' },
    { value: ContactRole.TECHNICAL, label: '기술담당' },
  ];

  let contacts = $state<ContactDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  
  let dialogOpen = $state(false);
  let dialogMode = $state<'add' | 'edit'>('add');
  let editingContact = $state<ContactDto | null>(null);
  let saving = $state(false);
  
  let formName = $state('');
  let formRole = $state<ContactRole>(ContactRole.SECONDARY);
  let formPhone = $state('');
  let formEmail = $state('');
  let formIsPrimary = $state(false);
  
  let deleteConfirmOpen = $state(false);
  let contactToDelete = $state<ContactDto | null>(null);
  let deleting = $state(false);

  let prevEntityId = $state<string | null>(null);

  $effect(() => {
    if (entityId && entityId !== prevEntityId && !loading) {
      prevEntityId = entityId;
      loadContacts();
    }
  });

  async function loadContacts() {
    loading = true;
    error = null;

    try {
      const response = await contactApi.listByEntity(entityType, entityId);
      if (response.success && response.data) {
        contacts = response.data;
      } else {
        error = response.error?.message || '담당자 목록을 불러올 수 없습니다.';
      }
    } catch (err) {
      error = '담당자 목록을 불러올 수 없습니다.';
    } finally {
      loading = false;
    }
  }

  function resetForm() {
    formName = '';
    formRole = ContactRole.SECONDARY;
    formPhone = '';
    formEmail = '';
    formIsPrimary = false;
  }

  function openAddDialog() {
    dialogMode = 'add';
    editingContact = null;
    resetForm();
    dialogOpen = true;
  }

  function openEditDialog(contact: ContactDto) {
    dialogMode = 'edit';
    editingContact = contact;
    formName = contact.name;
    formRole = contact.role;
    formPhone = contact.phone || '';
    formEmail = contact.email || '';
    formIsPrimary = contact.isPrimary;
    dialogOpen = true;
  }

  function closeDialog() {
    dialogOpen = false;
    editingContact = null;
    resetForm();
  }

  async function handleSave() {
    if (!formName.trim()) {
      return;
    }

    saving = true;
    error = null;

    try {
      if (dialogMode === 'add') {
        const request: ContactCreateRequest = {
          name: formName.trim(),
          phone: formPhone.trim() || undefined,
          email: formEmail.trim() || undefined,
          role: formRole,
          entityType,
          entityId
        };

        const response = await contactApi.create(request);
        if (response.success && response.data) {
          if (formIsPrimary) {
            await contactApi.setPrimary(response.data.id);
          }
          await loadContacts();
          closeDialog();
        } else {
          error = response.error?.message || '담당자 등록에 실패했습니다.';
        }
      } else if (editingContact) {
        const request: ContactUpdateRequest = {
          name: formName.trim(),
          phone: formPhone.trim() || undefined,
          email: formEmail.trim() || undefined,
          role: formRole
        };

        const response = await contactApi.update(editingContact.id, request);
        if (response.success) {
          if (formIsPrimary && !editingContact.isPrimary) {
            await contactApi.setPrimary(editingContact.id);
          }
          await loadContacts();
          closeDialog();
        } else {
          error = response.error?.message || '담당자 수정에 실패했습니다.';
        }
      }
    } catch (err) {
      error = dialogMode === 'add' ? '담당자 등록에 실패했습니다.' : '담당자 수정에 실패했습니다.';
    } finally {
      saving = false;
    }
  }

  function openDeleteConfirm(contact: ContactDto) {
    contactToDelete = contact;
    deleteConfirmOpen = true;
  }

  function closeDeleteConfirm() {
    deleteConfirmOpen = false;
    contactToDelete = null;
  }

  async function handleDelete() {
    if (!contactToDelete) return;

    deleting = true;
    error = null;

    try {
      const response = await contactApi.delete(contactToDelete.id);
      if (response.success) {
        await loadContacts();
        closeDeleteConfirm();
      } else {
        error = response.error?.message || '담당자 삭제에 실패했습니다.';
      }
    } catch (err) {
      error = '담당자 삭제에 실패했습니다.';
    } finally {
      deleting = false;
    }
  }

  async function handleSetPrimary(contact: ContactDto) {
    if (contact.isPrimary) return;

    error = null;

    try {
      const response = await contactApi.setPrimary(contact.id);
      if (response.success) {
        await loadContacts();
      } else {
        error = response.error?.message || '대표 담당자 설정에 실패했습니다.';
      }
    } catch (err) {
      error = '대표 담당자 설정에 실패했습니다.';
    }
  }

  function getRoleBadgeVariant(role: ContactRole): 'default' | 'secondary' | 'outline' {
    switch (role) {
      case ContactRole.PRIMARY:
        return 'default';
      case ContactRole.SETTLEMENT:
        return 'secondary';
      default:
        return 'outline';
    }
  }
</script>

<Card>
  <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-4">
    <CardTitle class="text-base font-semibold">담당자</CardTitle>
    <Button size="sm" onclick={openAddDialog}>
      <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M22 21v-2a4 4 0 00-3-3.87"/>
        <path d="M16 3.13a4 4 0 010 7.75"/>
      </svg>
      담당자 추가
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
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 8v4m0 4h.01"/>
        </svg>
        <span class="text-sm">{error}</span>
        <Button variant="outline" size="sm" onclick={loadContacts}>다시 시도</Button>
      </div>
    {:else if contacts.length === 0}
      <div class="flex flex-col items-center justify-center py-12 gap-4 text-muted-foreground bg-muted/30 rounded-lg border-2 border-dashed border-muted">
        <div class="w-14 h-14 rounded-full bg-muted flex items-center justify-center">
          <svg class="w-7 h-7 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 00-3-3.87"/>
            <path d="M16 3.13a4 4 0 010 7.75"/>
          </svg>
        </div>
        <div class="text-center">
          <p class="font-medium text-foreground">등록된 담당자가 없습니다</p>
          <p class="text-sm mt-1">담당자를 추가하여 연락처를 관리하세요</p>
        </div>
        <Button size="sm" onclick={openAddDialog}>
          <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14m-7-7h14"/>
          </svg>
          첫 담당자 추가
        </Button>
      </div>
    {:else}
      <div class="space-y-3">
        {#each contacts as contact (contact.id)}
          <div class={cn(
            "group relative p-4 rounded-lg border transition-all hover:shadow-md",
            contact.isPrimary 
              ? "border-primary/50 bg-primary/5 ring-1 ring-primary/20" 
              : "border-border bg-background hover:border-muted-foreground/30"
          )}>
            <div class="flex items-start justify-between gap-4">
              <div class="flex items-start gap-4 min-w-0 flex-1">
                <div class={cn(
                  "shrink-0 w-11 h-11 rounded-full flex items-center justify-center text-sm font-bold",
                  contact.isPrimary 
                    ? "bg-primary text-primary-foreground" 
                    : "bg-muted text-muted-foreground"
                )}>
                  {contact.name.slice(0, 1)}
                </div>
                
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-foreground">{contact.name}</span>
                    {#if contact.isPrimary}
                      <Badge variant="default" class="text-xs px-2 py-0.5">
                        <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                        </svg>
                        대표
                      </Badge>
                    {/if}
                    <Badge variant={getRoleBadgeVariant(contact.role)} class="text-xs">
                      {CONTACT_ROLE_LABELS[contact.role] || contact.role}
                    </Badge>
                  </div>
                  
                  <div class="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
                    {#if contact.phone}
                      <span class="flex items-center gap-1.5">
                        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/>
                        </svg>
                        {contact.phone}
                      </span>
                    {/if}
                    {#if contact.email}
                      <span class="flex items-center gap-1.5">
                        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                          <polyline points="22,6 12,13 2,6"/>
                        </svg>
                        {contact.email}
                      </span>
                    {/if}
                  </div>
                </div>
              </div>
              
              <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                {#if !contact.isPrimary}
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    class="h-8 px-2 text-xs"
                    onclick={() => handleSetPrimary(contact)}
                  >
                    <svg class="w-3.5 h-3.5 mr-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                    </svg>
                    대표
                  </Button>
                {/if}
                <Button 
                  variant="ghost" 
                  size="sm" 
                  class="h-8 w-8 p-0"
                  onclick={() => openEditDialog(contact)}
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </Button>
                <Button 
                  variant="ghost" 
                  size="sm" 
                  class="h-8 w-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                  onclick={() => openDeleteConfirm(contact)}
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18m-2 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                    <line x1="10" y1="11" x2="10" y2="17"/>
                    <line x1="14" y1="11" x2="14" y2="17"/>
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

<DialogPrimitive.Root open={dialogOpen} onOpenChange={(v) => { dialogOpen = v; if (!v) closeDialog(); }}>
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay 
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" 
    />
    <DialogPrimitive.Content
      class="fixed left-[50%] top-[50%] z-50 w-full max-w-md translate-x-[-50%] translate-y-[-50%] rounded-xl border bg-background shadow-2xl duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%]"
    >
      <div class="flex items-center justify-between px-6 py-4 border-b border-border">
        <div>
          <DialogPrimitive.Title class="text-lg font-bold text-foreground">
            {dialogMode === 'add' ? '담당자 추가' : '담당자 수정'}
          </DialogPrimitive.Title>
          <DialogPrimitive.Description class="text-sm text-muted-foreground mt-0.5">
            {dialogMode === 'add' ? '새로운 담당자 정보를 입력하세요' : '담당자 정보를 수정합니다'}
          </DialogPrimitive.Description>
        </div>
        <DialogPrimitive.Close class="rounded-full p-2 hover:bg-muted transition-colors">
          <svg class="w-5 h-5 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </DialogPrimitive.Close>
      </div>

      <div class="px-6 py-5 space-y-5">
        <div class="space-y-2">
          <Label for="contact-name" class="text-sm font-medium">이름 <span class="text-destructive">*</span></Label>
          <Input
            id="contact-name"
            type="text"
            placeholder="담당자 이름을 입력하세요"
            value={formName}
            oninput={(e) => formName = e.currentTarget.value}
          />
        </div>

        <div class="space-y-2">
          <Label for="role-select" class="text-sm font-medium">역할 <span class="text-destructive">*</span></Label>
          <Select.Root type="single" bind:value={formRole}>
            <Select.Trigger class="w-full">
              {#if formRole}
                {CONTACT_ROLE_LABELS[formRole]}
              {:else}
                <span class="text-muted-foreground">역할 선택</span>
              {/if}
            </Select.Trigger>
            <Select.Content>
              <Select.Item value={ContactRole.PRIMARY}>대표</Select.Item>
              <Select.Item value={ContactRole.SECONDARY}>담당자</Select.Item>
              <Select.Item value={ContactRole.SETTLEMENT}>정산담당</Select.Item>
              <Select.Item value={ContactRole.TECHNICAL}>기술담당</Select.Item>
            </Select.Content>
          </Select.Root>
        </div>

        <div class="space-y-2">
          <Label for="contact-phone" class="text-sm font-medium">연락처</Label>
          <Input
            id="contact-phone"
            type="tel"
            placeholder="010-0000-0000"
            value={formPhone}
            oninput={(e) => formPhone = e.currentTarget.value}
          />
        </div>

        <div class="space-y-2">
          <Label for="contact-email" class="text-sm font-medium">이메일</Label>
          <Input
            id="contact-email"
            type="email"
            placeholder="example@email.com"
            value={formEmail}
            oninput={(e) => formEmail = e.currentTarget.value}
          />
        </div>

        <label class="flex items-center gap-3 p-3 rounded-lg border border-border hover:bg-muted/50 cursor-pointer transition-colors">
          <input
            type="checkbox"
            bind:checked={formIsPrimary}
            class="w-4 h-4 rounded border-input text-primary focus:ring-primary focus:ring-offset-0"
          />
          <div>
            <span class="font-medium text-sm text-foreground">대표 담당자로 설정</span>
            <p class="text-xs text-muted-foreground mt-0.5">주요 연락처로 사용됩니다</p>
          </div>
        </label>
      </div>

      <div class="flex justify-end gap-3 px-6 py-4 border-t border-border bg-muted/30">
        <Button variant="outline" onclick={closeDialog} disabled={saving}>
          취소
        </Button>
        <Button 
          onclick={handleSave} 
          disabled={saving || !formName.trim()}
        >
          {#if saving}
            <div class="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin mr-2"></div>
          {/if}
          {dialogMode === 'add' ? '추가' : '저장'}
        </Button>
      </div>
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
</DialogPrimitive.Root>

<DialogPrimitive.Root open={deleteConfirmOpen} onOpenChange={(v) => { deleteConfirmOpen = v; if (!v) closeDeleteConfirm(); }}>
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
            <path d="M3 6h18m-2 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
          </svg>
        </div>
        
        <DialogPrimitive.Title class="text-lg font-bold text-foreground text-center">
          담당자 삭제
        </DialogPrimitive.Title>
        <DialogPrimitive.Description class="text-sm text-muted-foreground text-center mt-2">
          {#if contactToDelete}
            <span class="font-medium text-foreground">{contactToDelete.name}</span>
            <span> ({CONTACT_ROLE_LABELS[contactToDelete.role] || contactToDelete.role})</span>
            <br/>담당자를 삭제하시겠습니까?
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
