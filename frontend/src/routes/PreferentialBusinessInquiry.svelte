<script lang="ts">
  import { Button } from '$lib/components/ui/button';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';
  import { Badge } from '$lib/components/ui/badge';
  import { apiClient } from '../lib/api';
  import type { PreferentialBusinessResponse } from '../types/api';

  let businessNumbers = $state('');
  let results = $state<PreferentialBusinessResponse[]>([]);
  let loading = $state(false);
  let error = $state<string | null>(null);

  async function handleQuery() {
    const trimmed = businessNumbers.trim();
    
    if (!trimmed) {
      error = '사업자번호를 입력하세요.';
      return;
    }

    // Validate: max 10 business numbers
    const numbers = trimmed.split(',').map((n: string) => n.trim()).filter((n: string) => n);
    if (numbers.length > 10) {
      error = '사업자번호는 최대 10개까지 조회 가능합니다.';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await apiClient.get<PreferentialBusinessResponse[]>(
        `/preferential-business/query?businessNumbers=${encodeURIComponent(trimmed)}`
      );

      if (response.success && response.data) {
        results = response.data;
        if (results.length === 0) {
          error = '조회된 결과가 없습니다.';
        }
      } else {
        error = response.error?.message || '조회 실패';
        results = [];
      }
    } catch (err) {
      error = '요청 중 오류가 발생했습니다.';
      results = [];
      console.error('Preferential business query error:', err);
    } finally {
      loading = false;
    }
  }

  function handleReset() {
    businessNumbers = '';
    results = [];
    error = null;
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      handleQuery();
    }
  }

  function getGradeBadgeVariant(grade: string): 'default' | 'secondary' | 'outline' {
    switch (grade) {
      case '우대':
        return 'default';
      case '일반':
        return 'secondary';
      default:
        return 'outline';
    }
  }
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1200px] mx-auto">
  <!-- Header -->
  <header class="flex justify-between items-center">
    <div>
      <h1 class="text-2xl font-bold text-foreground tracking-tight">우대사업자 조회</h1>
      <p class="text-sm text-muted-foreground mt-1">국세청 우대 등급 조회</p>
    </div>
  </header>

  <!-- Input Section -->
  <Card class="border-border shadow-sm">
    <CardContent class="p-6">
      <div class="flex flex-col gap-5">
        <!-- Search Input -->
        <div class="flex flex-col gap-2">
          <Label for="businessNumbers" class="text-sm font-medium">사업자번호</Label>
          <div class="flex gap-3">
            <div class="relative flex-1">
              <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
              </svg>
              <Input
                id="businessNumbers"
                type="text"
                placeholder="사업자번호 입력 (예: 2068190716,3153400737)"
                value={businessNumbers}
                oninput={(e) => businessNumbers = e.currentTarget.value}
                onkeydown={handleKeydown}
                class="flex-1"
              />
            </div>
            <Button onclick={handleQuery} disabled={loading} class="h-11 px-6">
              {#if loading}
                <span class="flex items-center gap-2">
                  <span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                  조회 중...
                </span>
              {:else}
                조회
              {/if}
            </Button>
            <Button variant="outline" onclick={handleReset} class="h-11 px-5">
              초기화
            </Button>
          </div>
        </div>

        <!-- Warning Messages -->
        <div class="flex flex-col gap-2.5 p-4 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50 rounded-lg">
          <div class="flex items-start gap-2.5 text-sm text-amber-800 dark:text-amber-200">
            <svg class="w-5 h-5 flex-shrink-0 mt-0.5 text-amber-500" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
            </svg>
            <span>사업자번호 상태 (계속, 휴업, 폐업) 확인 불가합니다.</span>
          </div>
          <div class="flex items-start gap-2.5 text-sm text-amber-800 dark:text-amber-200">
            <svg class="w-5 h-5 flex-shrink-0 mt-0.5 text-amber-500" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
            </svg>
            <span>사업자번호 다중 조회시 최대 10개까지 <code class="px-1.5 py-0.5 bg-amber-100 dark:bg-amber-900/50 rounded text-xs font-mono">,</code> 구분으로 가능합니다. (예: 2068190716,3153400737)</span>
          </div>
        </div>

        <!-- Error Message -->
        {#if error}
          <div class="flex items-center gap-2.5 p-4 bg-destructive/10 border border-destructive/30 rounded-lg text-destructive text-sm">
            <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
            </svg>
            <span>{error}</span>
          </div>
        {/if}
      </div>
    </CardContent>
  </Card>

  <!-- Results Table -->
  {#if results.length > 0}
    <Card class="border-border shadow-sm overflow-hidden">
      <CardHeader class="pb-3 border-b border-border bg-muted/30">
        <div class="flex items-center justify-between">
          <CardTitle class="text-lg font-semibold">우대사업자 정보</CardTitle>
          <Badge variant="secondary" class="text-xs font-medium">
            총 {results.length}건
          </Badge>
        </div>
      </CardHeader>
      <CardContent class="p-0">
        <Table>
          <TableHeader>
            <TableRow class="bg-gradient-to-b from-muted/50 to-muted hover:bg-muted/60">
              <TableHead class="font-bold text-xs uppercase tracking-wide w-[200px]">사업자번호</TableHead>
              <TableHead class="font-bold text-xs uppercase tracking-wide w-[150px] text-center">영중소등급</TableHead>
              <TableHead class="font-bold text-xs uppercase tracking-wide text-center">2025년 상반기</TableHead>
              <TableHead class="font-bold text-xs uppercase tracking-wide text-center">2025년 하반기</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {#each results as result, i (result.businessNumber)}
              <TableRow class={i % 2 === 0 ? 'bg-background' : 'bg-muted/20'}>
                <TableCell class="font-mono text-sm font-medium text-primary">
                  {result.businessNumber}
                </TableCell>
                <TableCell class="text-center">
                  <Badge variant={getGradeBadgeVariant(result.smeCategoryGrade)} class="min-w-[60px]">
                    {result.smeCategoryGrade || '-'}
                  </Badge>
                </TableCell>
                <TableCell class="text-center">
                  <Badge variant="outline" class="min-w-[60px] bg-background">
                    {result.firstHalf2025Grade || '-'}
                  </Badge>
                </TableCell>
                <TableCell class="text-center">
                  <Badge variant="outline" class="min-w-[60px] bg-background">
                    {result.secondHalf2025Grade || '-'}
                  </Badge>
                </TableCell>
              </TableRow>
            {/each}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  {/if}

  <!-- Empty State (when no search has been made) -->
  {#if results.length === 0 && !error && !loading}
    <Card class="border-border shadow-sm">
      <CardContent class="py-16">
        <div class="flex flex-col items-center justify-center gap-4 text-center">
          <div class="w-16 h-16 rounded-full bg-muted/50 flex items-center justify-center">
            <svg class="w-8 h-8 text-muted-foreground" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
            </svg>
          </div>
          <div>
            <p class="text-base font-semibold text-muted-foreground">사업자번호를 입력하세요</p>
            <p class="text-sm text-muted-foreground/70 mt-1">최대 10개까지 쉼표(,)로 구분하여 조회할 수 있습니다</p>
          </div>
        </div>
      </CardContent>
    </Card>
  {/if}
</div>
