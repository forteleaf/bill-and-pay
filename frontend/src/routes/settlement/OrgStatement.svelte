<script lang="ts">
	import { Card, CardContent } from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import { Label } from '$lib/components/ui/label';
	import { Input } from '$lib/components/ui/input';
	import { DateRangePicker } from '$lib/components/ui/date-range-picker';
	import {
		Table,
		TableBody,
		TableCell,
		TableHead,
		TableHeader,
		TableRow
	} from '$lib/components/ui/table';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Separator } from '$lib/components/ui/separator';
	import { branchApi } from '@/api/branch';
	import { settlementApi } from '@/api/settlement';
	import { tenantStore } from '@/stores/tenant';
	import type { Branch } from '@/types/branch';
	import type { OrgStatement, DailyOrgStatementRow } from '@/types/api';
	import type { OrgStatementParams } from '@/types/settlement';

	// --- State ---
	let searchQuery = $state('');
	let searchResults = $state<Branch[]>([]);
	let showSearchResults = $state(false);
	let searching = $state(false);

	let selectedOrg = $state<Branch | null>(null);

	let startDate = $state('');
	let endDate = $state('');

	let statement = $state<OrgStatement | null>(null);
	let loading = $state(false);
	let error = $state<string | null>(null);

	// --- Org type labels ---
	const orgTypeLabels: Record<string, string> = {
		DISTRIBUTOR: '총판',
		AGENCY: '대리점',
		DEALER: '딜러',
		SELLER: '셀러'
	};

	function getOrgTypeLabel(orgType: string): string {
		return orgTypeLabels[orgType] || orgType;
	}

	// --- Date helpers ---
	function setDateRange(days: number) {
		const end = new Date();
		const start = new Date();
		start.setDate(end.getDate() - days);
		startDate = format(start, 'yyyy/MM/dd');
		endDate = format(end, 'yyyy/MM/dd');
	}

	function toISODate(dateStr: string): string {
		if (!dateStr) return '';
		return dateStr.replace(/\//g, '-');
	}

	// --- Formatting helpers ---
	function formatCurrency(amount: number | undefined | null): string {
		if (amount === undefined || amount === null) return '0';
		return new Intl.NumberFormat('ko-KR').format(amount);
	}

	function formatPercent(rate: number | undefined | null): string {
		if (rate === undefined || rate === null) return '0.00%';
		return rate.toFixed(2) + '%';
	}

	function formatDate(dateStr: string | undefined | null): string {
		if (!dateStr) return '-';
		try {
			const d = new Date(dateStr);
			return format(d, 'yyyy-MM-dd');
		} catch {
			return dateStr;
		}
	}

	// --- Search organizations ---
	let searchTimer: ReturnType<typeof setTimeout> | null = null;

	function handleSearchInput(e: Event) {
		const target = e.target as HTMLInputElement;
		searchQuery = target.value;

		if (searchTimer) clearTimeout(searchTimer);

		if (searchQuery.trim().length < 1) {
			searchResults = [];
			showSearchResults = false;
			return;
		}

		searchTimer = setTimeout(() => {
			searchOrganizations();
		}, 300);
	}

	async function searchOrganizations() {
		if (!searchQuery.trim()) return;

		searching = true;
		try {
			const response = await branchApi.getBranches({ search: searchQuery, size: 10 });
			if (response.success && response.data) {
				searchResults = (response.data.content || []).filter(
					(b: Branch) => b.orgType !== 'VENDOR'
				);
			} else {
				searchResults = [];
			}
			showSearchResults = true;
		} catch (err) {
			console.error('영업점 검색 오류:', err);
			searchResults = [];
		} finally {
			searching = false;
		}
	}

	function selectOrganization(org: Branch) {
		selectedOrg = org;
		searchQuery = '';
		searchResults = [];
		showSearchResults = false;
		statement = null;
		error = null;
	}

	function clearOrganization() {
		selectedOrg = null;
		statement = null;
		error = null;
	}

	// --- Fetch statement ---
	async function fetchStatement() {
		if (!selectedOrg) {
			error = '영업점을 선택해주세요.';
			return;
		}
		if (!startDate || !endDate) {
			error = '조회 기간을 선택해주세요.';
			return;
		}

		loading = true;
		error = null;
		statement = null;

		try {
			const params: OrgStatementParams = {
				orgId: selectedOrg.id,
				startDate: toISODate(startDate),
				endDate: toISODate(endDate)
			};
			const response = await settlementApi.getOrgStatement(params);
			if (response.success && response.data) {
				statement = response.data;
			} else {
				error = response.error?.message || '정산내역서를 불러올 수 없습니다.';
			}
		} catch (err) {
			error = err instanceof Error ? err.message : '정산내역서를 불러올 수 없습니다.';
			console.error('정산내역서 조회 오류:', err);
		} finally {
			loading = false;
		}
	}

	// --- Print ---
	function handlePrint() {
		window.print();
	}

	// --- Derived ---
	let canSearch = $derived(!!selectedOrg && !!startDate && !!endDate);
	let hasDetails = $derived(
		statement !== null && statement.dailyDetails && statement.dailyDetails.length > 0
	);
</script>

<div class="flex flex-col gap-6 p-6">
	<!-- Header -->
	<div class="flex items-center justify-between print:hidden">
		<div>
			<h1 class="text-2xl font-bold text-foreground">영업점 정산내역서</h1>
			<p class="mt-1 text-sm text-muted-foreground">
				영업점별 수수료 수익 및 정산 내역을 조회합니다.
			</p>
		</div>
		{#if statement}
			<Button variant="outline" size="sm" onclick={handlePrint}>
				<svg
					xmlns="http://www.w3.org/2000/svg"
					class="mr-2 h-4 w-4"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					stroke-linecap="round"
					stroke-linejoin="round"
				>
					<polyline points="6 9 6 2 18 2 18 9" />
					<path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
					<rect x="6" y="14" width="12" height="8" />
				</svg>
				인쇄
			</Button>
		{/if}
	</div>

	<!-- Search & Filters -->
	<Card class="print:hidden">
		<CardContent class="pt-6">
			<div class="flex flex-col gap-4">
				<!-- Organization Search -->
				<div class="flex flex-col gap-1.5">
					<Label>영업점 검색</Label>
					{#if selectedOrg}
						<div class="flex items-center gap-2">
							<Badge variant="secondary" class="flex items-center gap-1.5 px-3 py-1.5 text-sm">
								<span class="font-medium">{selectedOrg.name}</span>
								<span class="text-muted-foreground">({selectedOrg.orgCode})</span>
								<button
									type="button"
									class="ml-1 rounded-full p-0.5 hover:bg-muted"
									aria-label="선택 해제"
									onclick={clearOrganization}
								>
									<svg
										xmlns="http://www.w3.org/2000/svg"
										class="h-3.5 w-3.5"
										viewBox="0 0 24 24"
										fill="none"
										stroke="currentColor"
										stroke-width="2"
										stroke-linecap="round"
										stroke-linejoin="round"
									>
										<line x1="18" y1="6" x2="6" y2="18" />
										<line x1="6" y1="6" x2="18" y2="18" />
									</svg>
								</button>
							</Badge>
						</div>
					{:else}
						<div class="relative">
							<Input
								type="text"
								placeholder="영업점명 또는 코드를 입력하세요"
								value={searchQuery}
								oninput={handleSearchInput}
								onfocus={() => {
									if (searchResults.length > 0) showSearchResults = true;
								}}
								class="w-full max-w-md"
							/>
							{#if showSearchResults && searchResults.length > 0}
								<div
									class="absolute top-full z-50 mt-1 w-full max-w-md rounded-md border border-border bg-background shadow-lg"
								>
									<Table>
										<TableHeader>
											<TableRow>
												<TableHead class="text-xs">영업점명</TableHead>
												<TableHead class="text-xs">코드</TableHead>
												<TableHead class="text-xs">유형</TableHead>
												<TableHead class="text-xs">상태</TableHead>
											</TableRow>
										</TableHeader>
										<TableBody>
											{#each searchResults as org}
												<TableRow
													class="cursor-pointer hover:bg-muted/50"
													onclick={() => selectOrganization(org)}
												>
													<TableCell class="py-2 text-sm font-medium">{org.name}</TableCell>
													<TableCell class="py-2 text-sm text-muted-foreground">
														{org.orgCode}
													</TableCell>
													<TableCell class="py-2">
														<Badge variant="outline" class="text-xs">
															{getOrgTypeLabel(org.orgType)}
														</Badge>
													</TableCell>
													<TableCell class="py-2">
														<Badge
															variant={org.status === 'ACTIVE' ? 'default' : 'secondary'}
															class="text-xs"
														>
															{org.status === 'ACTIVE' ? '활성' : '비활성'}
														</Badge>
													</TableCell>
												</TableRow>
											{/each}
										</TableBody>
									</Table>
								</div>
							{/if}
							{#if showSearchResults && searchResults.length === 0 && searchQuery.trim() && !searching}
								<div
									class="absolute top-full z-50 mt-1 w-full max-w-md rounded-md border border-border bg-background p-4 text-center text-sm text-muted-foreground shadow-lg"
								>
									검색 결과가 없습니다.
								</div>
							{/if}
						</div>
					{/if}
				</div>

				<Separator />

				<!-- Date Range -->
				<div class="flex flex-row flex-wrap items-end gap-3">
					<div class="flex flex-col gap-1.5">
						<Label>기간</Label>
						<DateRangePicker
							{startDate}
							{endDate}
							onchange={(start, end) => {
								startDate = start;
								endDate = end;
							}}
							placeholder="기간 선택"
							class="w-[280px]"
						/>
					</div>
					<div class="flex gap-1">
						<Button variant="outline" size="sm" onclick={() => setDateRange(7)}>7일</Button>
						<Button variant="outline" size="sm" onclick={() => setDateRange(30)}>30일</Button>
						<Button variant="outline" size="sm" onclick={() => setDateRange(90)}>90일</Button>
					</div>
					<Button onclick={fetchStatement} disabled={!canSearch || loading}>
						{#if loading}
							<svg
								class="mr-2 h-4 w-4 animate-spin"
								xmlns="http://www.w3.org/2000/svg"
								fill="none"
								viewBox="0 0 24 24"
							>
								<circle
									class="opacity-25"
									cx="12"
									cy="12"
									r="10"
									stroke="currentColor"
									stroke-width="4"
								/>
								<path
									class="opacity-75"
									fill="currentColor"
									d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
								/>
							</svg>
							조회 중...
						{:else}
							조회
						{/if}
					</Button>
				</div>
			</div>
		</CardContent>
	</Card>

	<!-- Click outside to close search results -->
	{#if showSearchResults}
		<button
			type="button"
			class="fixed inset-0 z-40"
			onclick={() => (showSearchResults = false)}
			aria-label="검색 결과 닫기"
		></button>
	{/if}

	<!-- Error -->
	{#if error}
		<Card class="border-destructive">
			<CardContent class="pt-6">
				<div class="flex items-center gap-2 text-destructive">
					<svg
						xmlns="http://www.w3.org/2000/svg"
						class="h-5 w-5"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						stroke-linecap="round"
						stroke-linejoin="round"
					>
						<circle cx="12" cy="12" r="10" />
						<line x1="15" y1="9" x2="9" y2="15" />
						<line x1="9" y1="9" x2="15" y2="15" />
					</svg>
					<span class="text-sm font-medium">{error}</span>
				</div>
			</CardContent>
		</Card>
	{/if}

	<!-- Loading Skeleton -->
	{#if loading}
		<div class="flex flex-col gap-6">
			<Card>
				<CardContent class="pt-6">
					<div class="flex flex-col gap-4">
						<Skeleton class="h-6 w-32" />
						<div class="grid grid-cols-2 gap-4 md:grid-cols-3">
							{#each Array(6) as _}
								<div class="flex flex-col gap-1">
									<Skeleton class="h-4 w-20" />
									<Skeleton class="h-5 w-32" />
								</div>
							{/each}
						</div>
					</div>
				</CardContent>
			</Card>
			<Card>
				<CardContent class="pt-6">
					<Skeleton class="h-6 w-40" />
					<div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
						{#each Array(3) as _}
							<Skeleton class="h-24 w-full rounded-lg" />
						{/each}
					</div>
				</CardContent>
			</Card>
			<Card>
				<CardContent class="pt-6">
					<Skeleton class="h-6 w-32" />
					<div class="mt-4 flex flex-col gap-2">
						{#each Array(5) as _}
							<Skeleton class="h-10 w-full" />
						{/each}
					</div>
				</CardContent>
			</Card>
		</div>
	{/if}

	<!-- Statement Content -->
	{#if statement && !loading}
		<!-- Print Header (visible only on print) -->
		<div class="hidden print:block print:mb-6">
			<h1 class="text-center text-2xl font-bold">영업점 정산내역서</h1>
			<p class="mt-1 text-center text-sm text-muted-foreground">
				조회기간: {statement.periodStart} ~ {statement.periodEnd}
			</p>
		</div>

		<!-- Organization Info -->
		<Card>
			<CardContent class="pt-6">
				<h2 class="mb-4 text-lg font-semibold text-foreground">영업점 정보</h2>
				<div class="grid grid-cols-2 gap-x-8 gap-y-3 md:grid-cols-3">
					<div class="flex flex-col gap-0.5">
						<span class="text-xs text-muted-foreground">영업점명</span>
						<span class="text-sm font-medium">{statement.orgName}</span>
					</div>
					<div class="flex flex-col gap-0.5">
						<span class="text-xs text-muted-foreground">영업점코드</span>
						<span class="text-sm font-medium">{statement.orgCode}</span>
					</div>
					<div class="flex flex-col gap-0.5">
						<span class="text-xs text-muted-foreground">유형</span>
						<Badge variant="outline" class="w-fit text-xs">
							{getOrgTypeLabel(statement.orgType)}
						</Badge>
					</div>
					<div class="flex flex-col gap-0.5">
						<span class="text-xs text-muted-foreground">사업자번호</span>
						<span class="text-sm font-medium">
							{statement.businessNumber || '-'}
						</span>
					</div>
					<div class="flex flex-col gap-0.5">
						<span class="text-xs text-muted-foreground">대표자</span>
						<span class="text-sm font-medium">
							{statement.representativeName || '-'}
						</span>
					</div>
					<div class="flex flex-col gap-0.5">
						<span class="text-xs text-muted-foreground">정산계좌</span>
						<span class="text-sm font-medium">
							{#if statement.bankName && statement.accountNumber}
								{statement.bankName} {statement.accountNumber}
								{#if statement.accountHolder}
									({statement.accountHolder})
								{/if}
							{:else}
								-
							{/if}
						</span>
					</div>
				</div>
			</CardContent>
		</Card>

		<!-- Summary -->
		<Card>
			<CardContent class="pt-6">
				<h2 class="mb-4 text-lg font-semibold text-foreground">정산 요약</h2>
				<div class="grid grid-cols-1 gap-4 md:grid-cols-3">
					<!-- Total Fee Income -->
					<div class="rounded-lg border border-border bg-muted/30 p-4">
						<div class="flex flex-col gap-1">
							<span class="text-xs text-muted-foreground">총 수수료수익</span>
							<span class="text-2xl font-bold text-foreground">
								{formatCurrency(statement.summary.netAmount)}
								<span class="text-sm font-normal text-muted-foreground">원</span>
							</span>
						</div>
						<Separator class="my-3" />
						<div class="flex flex-col gap-1.5 text-xs">
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">승인금액</span>
								<span class="font-medium">
									{formatCurrency(statement.summary.totalApprovalAmount)}원
								</span>
							</div>
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">취소금액</span>
								<span class="font-medium text-destructive">
									-{formatCurrency(statement.summary.totalCancelAmount)}원
								</span>
							</div>
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">순매출</span>
								<span class="font-medium">
									{formatCurrency(statement.summary.grossAmount)}원
								</span>
							</div>
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">수수료금액</span>
								<span class="font-medium">
									{formatCurrency(statement.summary.feeAmount)}원
								</span>
							</div>
						</div>
					</div>

					<!-- Transaction Count -->
					<div class="rounded-lg border border-border bg-muted/30 p-4">
						<div class="flex flex-col gap-1">
							<span class="text-xs text-muted-foreground">거래건수</span>
							<span class="text-2xl font-bold text-foreground">
								{formatCurrency(statement.summary.transactionCount)}
								<span class="text-sm font-normal text-muted-foreground">건</span>
							</span>
						</div>
						<Separator class="my-3" />
						<div class="flex flex-col gap-1.5 text-xs">
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">승인</span>
								<span class="font-medium">
									{formatCurrency(statement.summary.totalApprovalCount)}건
								</span>
							</div>
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">취소</span>
								<span class="font-medium text-destructive">
									{formatCurrency(statement.summary.totalCancelCount)}건
								</span>
							</div>
						</div>
					</div>

					<!-- Average Fee Rate -->
					<div class="rounded-lg border border-border bg-muted/30 p-4">
						<div class="flex flex-col gap-1">
							<span class="text-xs text-muted-foreground">평균수수료율</span>
							<span class="text-2xl font-bold text-foreground">
								{formatPercent(statement.summary.feeRate)}
							</span>
						</div>
						<Separator class="my-3" />
						<div class="flex flex-col gap-1.5 text-xs">
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">정산주기</span>
								<span class="font-medium">{statement.settlementCycle || '-'}</span>
							</div>
							<div class="flex items-center justify-between">
								<span class="text-muted-foreground">조회기간</span>
								<span class="font-medium">
									{statement.periodStart} ~ {statement.periodEnd}
								</span>
							</div>
						</div>
					</div>
				</div>
			</CardContent>
		</Card>

		<!-- Daily Details Table -->
		<Card>
			<CardContent class="pt-6">
				<div class="mb-4 flex items-center justify-between">
					<h2 class="text-lg font-semibold text-foreground">일별 상세내역</h2>
					{#if hasDetails}
						<span class="text-xs text-muted-foreground">
							총 {statement.dailyDetails.length}건
						</span>
					{/if}
				</div>

				{#if hasDetails}
					<div class="overflow-x-auto rounded-md border border-border">
						<Table>
							<TableHeader>
								<TableRow>
									<TableHead class="text-center text-xs">정산일</TableHead>
									<TableHead class="text-center text-xs">거래일</TableHead>
									<TableHead class="text-center text-xs">건수</TableHead>
									<TableHead class="text-right text-xs">승인금액</TableHead>
									<TableHead class="text-right text-xs">취소금액</TableHead>
									<TableHead class="text-right text-xs">수수료금액</TableHead>
									<TableHead class="text-right text-xs">수수료수익</TableHead>
								</TableRow>
							</TableHeader>
							<TableBody>
								{#each statement.dailyDetails as row}
									<TableRow>
										<TableCell class="text-center text-sm">
											{formatDate(row.settlementDate)}
										</TableCell>
										<TableCell class="text-center text-sm">
											{formatDate(row.transactionDate)}
										</TableCell>
										<TableCell class="text-center text-sm">
											{formatCurrency(row.transactionCount)}
										</TableCell>
										<TableCell class="text-right text-sm">
											{formatCurrency(row.approvalAmount)}
										</TableCell>
										<TableCell class="text-right text-sm text-destructive">
											{#if row.cancelAmount > 0}
												-{formatCurrency(row.cancelAmount)}
											{:else}
												0
											{/if}
										</TableCell>
										<TableCell class="text-right text-sm">
											{formatCurrency(row.feeAmount)}
										</TableCell>
										<TableCell class="text-right text-sm font-medium">
											{formatCurrency(row.netAmount)}
										</TableCell>
									</TableRow>
								{/each}
								<!-- Totals Row -->
								<TableRow class="border-t-2 border-border bg-muted/50 font-semibold">
									<TableCell class="text-center text-sm" colspan={2}>합계</TableCell>
									<TableCell class="text-center text-sm">
										{formatCurrency(statement.summary.transactionCount)}
									</TableCell>
									<TableCell class="text-right text-sm">
										{formatCurrency(statement.summary.totalApprovalAmount)}
									</TableCell>
									<TableCell class="text-right text-sm text-destructive">
										{#if statement.summary.totalCancelAmount > 0}
											-{formatCurrency(statement.summary.totalCancelAmount)}
										{:else}
											0
										{/if}
									</TableCell>
									<TableCell class="text-right text-sm">
										{formatCurrency(statement.summary.feeAmount)}
									</TableCell>
									<TableCell class="text-right text-sm font-bold">
										{formatCurrency(statement.summary.netAmount)}
									</TableCell>
								</TableRow>
							</TableBody>
						</Table>
					</div>
				{:else}
					<div class="flex flex-col items-center justify-center rounded-lg border border-dashed border-border py-12">
						<svg
							xmlns="http://www.w3.org/2000/svg"
							class="mb-3 h-10 w-10 text-muted-foreground/50"
							viewBox="0 0 24 24"
							fill="none"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						>
							<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
							<polyline points="14 2 14 8 20 8" />
							<line x1="16" y1="13" x2="8" y2="13" />
							<line x1="16" y1="17" x2="8" y2="17" />
							<polyline points="10 9 9 9 8 9" />
						</svg>
						<p class="text-sm text-muted-foreground">해당 기간의 정산 내역이 없습니다.</p>
					</div>
				{/if}
			</CardContent>
		</Card>
	{/if}

	<!-- Empty state (no query yet) -->
	{#if !statement && !loading && !error}
		<Card>
			<CardContent class="pt-6">
				<div class="flex flex-col items-center justify-center py-16">
					<svg
						xmlns="http://www.w3.org/2000/svg"
						class="mb-4 h-12 w-12 text-muted-foreground/40"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="1.5"
						stroke-linecap="round"
						stroke-linejoin="round"
					>
						<rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
						<line x1="8" y1="21" x2="16" y2="21" />
						<line x1="12" y1="17" x2="12" y2="21" />
					</svg>
					<p class="text-base font-medium text-muted-foreground">
						영업점을 선택하고 기간을 설정한 후 조회해주세요.
					</p>
					<p class="mt-1 text-sm text-muted-foreground/70">
						영업점별 수수료 수익 및 일별 정산 상세내역을 확인할 수 있습니다.
					</p>
				</div>
			</CardContent>
		</Card>
	{/if}
</div>
