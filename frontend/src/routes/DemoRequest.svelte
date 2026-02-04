<script lang="ts">
  import { Button } from '$lib/components/ui/button';
  import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '$lib/components/ui/card';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Badge } from '$lib/components/ui/badge';

  interface Props {
    onBack: () => void;
  }
  
  let { onBack }: Props = $props();
  
  let formData = $state({
    companyName: '',
    name: '',
    email: '',
    phone: '',
    position: '',
    employeeCount: '',
    monthlyVolume: '',
    message: ''
  });
  
  let loading = $state(false);
  let submitted = $state(false);
  let error = $state<string | null>(null);
  
  const employeeOptions = [
    { value: '1-10', label: '1-10명' },
    { value: '11-50', label: '11-50명' },
    { value: '51-200', label: '51-200명' },
    { value: '201-500', label: '201-500명' },
    { value: '500+', label: '500명 이상' }
  ];
  
  const volumeOptions = [
    { value: 'under-100m', label: '1억 미만' },
    { value: '100m-500m', label: '1억 - 5억' },
    { value: '500m-1b', label: '5억 - 10억' },
    { value: '1b-5b', label: '10억 - 50억' },
    { value: 'over-5b', label: '50억 이상' }
  ];

  async function handleSubmit(e: Event) {
    e.preventDefault();
    
    if (!formData.companyName || !formData.name || !formData.email || !formData.phone) {
      error = '필수 항목을 모두 입력해주세요.';
      return;
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      error = '올바른 이메일 주소를 입력해주세요.';
      return;
    }
    
    loading = true;
    error = null;
    
    try {
      await new Promise(resolve => setTimeout(resolve, 1500));
      submitted = true;
    } catch (err) {
      error = '신청 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    } finally {
      loading = false;
    }
  }
  
  function formatPhone(value: string): string {
    const numbers = value.replace(/\D/g, '');
    if (numbers.length <= 3) return numbers;
    if (numbers.length <= 7) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
    return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7, 11)}`;
  }
  
  function handlePhoneInput(e: Event) {
    const target = e.target as HTMLInputElement;
    formData.phone = formatPhone(target.value);
  }
</script>

<div class="min-h-screen bg-background">
  <header class="fixed top-0 left-0 right-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
    <div class="container mx-auto px-4 h-16 flex items-center justify-between">
      <button onclick={onBack} class="flex items-center gap-2 hover:opacity-80 transition-opacity">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-purple-600 flex items-center justify-center">
          <span class="text-white font-bold text-sm">B&P</span>
        </div>
        <span class="text-xl font-bold text-foreground">Bill&Pay</span>
      </button>
      <Button variant="outline" onclick={onBack}>돌아가기</Button>
    </div>
  </header>

  <main class="pt-24 pb-16 px-4">
    <div class="container mx-auto max-w-6xl">
      {#if submitted}
        <div class="max-w-lg mx-auto text-center py-16">
          <div class="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-6">
            <svg class="w-10 h-10 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-foreground mb-4">신청이 완료되었습니다!</h1>
          <p class="text-muted-foreground mb-8">
            담당자가 영업일 기준 1-2일 내에 연락드리겠습니다.<br />
            빠른 상담을 원하시면 <a href="mailto:sales@billpay.kr" class="text-primary hover:underline">sales@billpay.kr</a>로 문의해주세요.
          </p>
          <Button onclick={onBack} size="lg">홈으로 돌아가기</Button>
        </div>
      {:else}
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-12">
          <div>
            <Badge variant="secondary" class="mb-4">무료 데모 체험</Badge>
            <h1 class="text-3xl md:text-4xl font-bold text-foreground mb-4">
              Bill&Pay를 직접<br />경험해보세요
            </h1>
            <p class="text-lg text-muted-foreground mb-8">
              전문 담당자가 귀사의 정산 환경에 맞는 맞춤형 데모를 제공해드립니다.
              30분 내외의 온라인 미팅으로 Bill&Pay의 모든 기능을 확인하세요.
            </p>
            
            <div class="space-y-6">
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <span class="text-xl">🎯</span>
                </div>
                <div>
                  <h3 class="font-semibold text-foreground mb-1">맞춤형 데모</h3>
                  <p class="text-sm text-muted-foreground">귀사의 비즈니스 환경과 요구사항에 맞춘 1:1 데모를 제공합니다.</p>
                </div>
              </div>
              
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <span class="text-xl">💡</span>
                </div>
                <div>
                  <h3 class="font-semibold text-foreground mb-1">전문가 상담</h3>
                  <p class="text-sm text-muted-foreground">정산 전문가가 최적의 솔루션을 제안해드립니다.</p>
                </div>
              </div>
              
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <span class="text-xl">🚀</span>
                </div>
                <div>
                  <h3 class="font-semibold text-foreground mb-1">빠른 도입</h3>
                  <p class="text-sm text-muted-foreground">데모 후 2주 이내 빠른 도입이 가능합니다.</p>
                </div>
              </div>
            </div>
          </div>
          
          <div>
            <Card class="shadow-lg">
              <CardHeader>
                <CardTitle>데모 신청</CardTitle>
                <CardDescription>아래 정보를 입력해주시면 담당자가 연락드립니다.</CardDescription>
              </CardHeader>
              <CardContent>
                <form onsubmit={handleSubmit} class="space-y-4">
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div class="space-y-2">
                      <Label for="companyName">회사명 <span class="text-destructive">*</span></Label>
                      <Input
                        id="companyName"
                        type="text"
                        bind:value={formData.companyName}
                        placeholder="(주)회사명"
                        disabled={loading}
                      />
                    </div>
                    
                    <div class="space-y-2">
                      <Label for="name">담당자명 <span class="text-destructive">*</span></Label>
                      <Input
                        id="name"
                        type="text"
                        bind:value={formData.name}
                        placeholder="홍길동"
                        disabled={loading}
                      />
                    </div>
                  </div>
                  
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div class="space-y-2">
                      <Label for="email">이메일 <span class="text-destructive">*</span></Label>
                      <Input
                        id="email"
                        type="email"
                        bind:value={formData.email}
                        placeholder="example@company.com"
                        disabled={loading}
                      />
                    </div>
                    
                    <div class="space-y-2">
                      <Label for="phone">연락처 <span class="text-destructive">*</span></Label>
                      <Input
                        id="phone"
                        type="tel"
                        value={formData.phone}
                        oninput={handlePhoneInput}
                        placeholder="010-1234-5678"
                        disabled={loading}
                      />
                    </div>
                  </div>
                  
                  <div class="space-y-2">
                    <Label for="position">직책</Label>
                    <Input
                      id="position"
                      type="text"
                      bind:value={formData.position}
                      placeholder="팀장"
                      disabled={loading}
                    />
                  </div>
                  
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div class="space-y-2">
                      <Label for="employeeCount">임직원 수</Label>
                      <select
                        id="employeeCount"
                        bind:value={formData.employeeCount}
                        disabled={loading}
                        class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <option value="">선택해주세요</option>
                        {#each employeeOptions as option}
                          <option value={option.value}>{option.label}</option>
                        {/each}
                      </select>
                    </div>
                    
                    <div class="space-y-2">
                      <Label for="monthlyVolume">월 거래액</Label>
                      <select
                        id="monthlyVolume"
                        bind:value={formData.monthlyVolume}
                        disabled={loading}
                        class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <option value="">선택해주세요</option>
                        {#each volumeOptions as option}
                          <option value={option.value}>{option.label}</option>
                        {/each}
                      </select>
                    </div>
                  </div>
                  
                  <div class="space-y-2">
                    <Label for="message">문의사항</Label>
                    <textarea
                      id="message"
                      bind:value={formData.message}
                      placeholder="궁금한 점이나 요청사항을 입력해주세요."
                      disabled={loading}
                      rows="3"
                      class="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 resize-none"
                    ></textarea>
                  </div>
                  
                  {#if error}
                    <div class="p-3 rounded-md bg-destructive/10 border border-destructive/20 text-destructive text-sm">
                      {error}
                    </div>
                  {/if}
                  
                  <Button type="submit" disabled={loading} class="w-full" size="lg">
                    {loading ? '신청 중...' : '데모 신청하기'}
                  </Button>
                  
                  <p class="text-xs text-muted-foreground text-center">
                    신청 시 <span class="text-foreground">개인정보처리방침</span>에 동의하는 것으로 간주됩니다.
                  </p>
                </form>
              </CardContent>
            </Card>
          </div>
        </div>
      {/if}
    </div>
  </main>
</div>
