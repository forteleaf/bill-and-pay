<script lang="ts">
  import { Button } from '$lib/components/ui/button';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Badge } from '$lib/components/ui/badge';

  interface Props {
    onLogin: () => void;
    onDemo: () => void;
  }
  
  let { onLogin, onDemo }: Props = $props();
  
  const features = [
    {
      icon: '📊',
      title: '자동 정산 시스템',
      description: '복잡한 수수료 체계를 자동으로 계산하고 정산합니다. 복식부기 기반 Zero-Sum 원칙으로 정확한 정산을 보장합니다.'
    },
    {
      icon: '🏢',
      title: '다단계 영업 구조',
      description: '총판 > 대리점 > 딜러 > 판매점 > 가맹점까지 5단계 계층 구조를 완벽하게 지원합니다.'
    },
    {
      icon: '⚡',
      title: '실시간 모니터링',
      description: '거래 현황, 정산 상태, 매출 통계를 실시간으로 확인하고 대시보드에서 한눈에 파악할 수 있습니다.'
    },
    {
      icon: '🔒',
      title: '멀티테넌트 보안',
      description: 'Schema-per-Tenant 방식으로 완벽한 데이터 격리와 보안을 제공합니다.'
    },
    {
      icon: '🔗',
      title: 'PG 연동 자동화',
      description: 'KORPAY 등 외부 PG사와의 웹훅 연동으로 결제 데이터를 자동으로 수집하고 처리합니다.'
    },
    {
      icon: '📈',
      title: '정산 리포트',
      description: '일별, 월별, 가맹점별 상세 정산 리포트를 제공하여 투명한 정산 관리가 가능합니다.'
    }
  ];
  
  const stats = [
    { value: '99.9%', label: '서비스 가동률' },
    { value: '< 0.1초', label: '정산 처리 속도' },
    { value: '100%', label: '정산 정확도' },
    { value: '24/7', label: '모니터링' }
  ];
</script>

<div class="min-h-screen bg-background">
  <!-- Header -->
  <header class="fixed top-0 left-0 right-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
    <div class="container mx-auto px-4 h-16 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-purple-600 flex items-center justify-center">
          <span class="text-white font-bold text-sm">B&P</span>
        </div>
        <span class="text-xl font-bold text-foreground">Bill&Pay</span>
      </div>
      <nav class="hidden md:flex items-center gap-8">
        <a href="#features" class="text-sm text-muted-foreground hover:text-foreground transition-colors">기능</a>
        <a href="#pricing" class="text-sm text-muted-foreground hover:text-foreground transition-colors">가격</a>
        <a href="#contact" class="text-sm text-muted-foreground hover:text-foreground transition-colors">문의</a>
      </nav>
      <Button onclick={onLogin}>로그인</Button>
    </div>
  </header>

  <!-- Hero Section -->
  <section class="pt-32 pb-20 px-4">
    <div class="container mx-auto text-center max-w-4xl">
      <Badge variant="secondary" class="mb-6">정산 자동화 솔루션</Badge>
      <h1 class="text-4xl md:text-6xl font-bold text-foreground mb-6 leading-tight">
        복잡한 정산,<br />
        <span class="bg-gradient-to-r from-primary to-purple-600 bg-clip-text text-transparent">Bill&Pay</span>로 간단하게
      </h1>
      <p class="text-lg md:text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
        다단계 영업 구조의 복잡한 수수료 체계를 자동화하고,
        외부 PG사 결제 데이터를 원장 기반으로 정확하게 정산하는 플랫폼
      </p>
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <Button size="lg" onclick={onLogin} class="text-lg px-8">
          무료로 시작하기
        </Button>
        <Button variant="outline" size="lg" onclick={onDemo} class="text-lg px-8">
          데모 요청
        </Button>
      </div>
    </div>
  </section>

  <!-- Stats Section -->
  <section class="py-16 bg-muted/50">
    <div class="container mx-auto px-4">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
        {#each stats as stat}
          <div class="text-center">
            <div class="text-3xl md:text-4xl font-bold text-primary mb-2">{stat.value}</div>
            <div class="text-sm text-muted-foreground">{stat.label}</div>
          </div>
        {/each}
      </div>
    </div>
  </section>

  <!-- Features Section -->
  <section id="features" class="py-20 px-4">
    <div class="container mx-auto max-w-6xl">
      <div class="text-center mb-16">
        <Badge variant="secondary" class="mb-4">핵심 기능</Badge>
        <h2 class="text-3xl md:text-4xl font-bold text-foreground mb-4">
          정산의 모든 것을 하나로
        </h2>
        <p class="text-muted-foreground max-w-2xl mx-auto">
          Bill&Pay는 복잡한 정산 업무를 단순화하고 자동화하여
          비즈니스에 집중할 수 있도록 돕습니다.
        </p>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {#each features as feature}
          <Card class="group hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
            <CardHeader>
              <div class="w-12 h-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4 group-hover:bg-primary/20 transition-colors">
                <span class="text-2xl">{feature.icon}</span>
              </div>
              <CardTitle class="text-lg">{feature.title}</CardTitle>
            </CardHeader>
            <CardContent>
              <p class="text-muted-foreground text-sm">{feature.description}</p>
            </CardContent>
          </Card>
        {/each}
      </div>
    </div>
  </section>

  <!-- How It Works Section -->
  <section class="py-20 px-4 bg-muted/30">
    <div class="container mx-auto max-w-6xl">
      <div class="text-center mb-16">
        <Badge variant="secondary" class="mb-4">작동 방식</Badge>
        <h2 class="text-3xl md:text-4xl font-bold text-foreground mb-4">
          3단계로 완성되는 자동 정산
        </h2>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div class="relative">
          <div class="absolute top-8 left-1/2 w-full h-0.5 bg-border hidden md:block"></div>
          <div class="relative z-10 text-center">
            <div class="w-16 h-16 rounded-full bg-primary text-primary-foreground flex items-center justify-center mx-auto mb-6 text-2xl font-bold">
              1
            </div>
            <h3 class="text-xl font-semibold mb-3">PG 데이터 수집</h3>
            <p class="text-muted-foreground text-sm">
              외부 PG사로부터 결제/취소 웹훅을 자동으로 수신하고 검증합니다.
            </p>
          </div>
        </div>
        
        <div class="relative">
          <div class="absolute top-8 left-1/2 w-full h-0.5 bg-border hidden md:block"></div>
          <div class="relative z-10 text-center">
            <div class="w-16 h-16 rounded-full bg-primary text-primary-foreground flex items-center justify-center mx-auto mb-6 text-2xl font-bold">
              2
            </div>
            <h3 class="text-xl font-semibold mb-3">수수료 자동 계산</h3>
            <p class="text-muted-foreground text-sm">
              계층별 수수료 정책에 따라 정산 금액을 자동으로 산출합니다.
            </p>
          </div>
        </div>
        
        <div class="relative">
          <div class="relative z-10 text-center">
            <div class="w-16 h-16 rounded-full bg-primary text-primary-foreground flex items-center justify-center mx-auto mb-6 text-2xl font-bold">
              3
            </div>
            <h3 class="text-xl font-semibold mb-3">정산 원장 생성</h3>
            <p class="text-muted-foreground text-sm">
              복식부기 원칙에 따라 Zero-Sum이 보장되는 정산 원장을 생성합니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- CTA Section -->
  <section class="py-20 px-4">
    <div class="container mx-auto max-w-4xl">
      <Card class="bg-gradient-to-br from-primary to-purple-600 text-primary-foreground border-0">
        <CardContent class="p-12 text-center">
          <h2 class="text-3xl md:text-4xl font-bold mb-4">
            지금 바로 시작하세요
          </h2>
          <p class="text-lg opacity-90 mb-8 max-w-2xl mx-auto">
            Bill&Pay와 함께 정산 업무를 혁신하고,
            비즈니스 성장에 집중하세요.
          </p>
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <Button size="lg" variant="secondary" onclick={onLogin} class="text-lg px-8">
              무료 체험 시작
            </Button>
            <Button size="lg" variant="outline" onclick={onDemo} class="text-lg px-8 bg-transparent border-white/30 text-white hover:bg-white/10">
              영업팀 문의
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  </section>

  <!-- Footer -->
  <footer id="contact" class="py-12 px-4 border-t bg-muted/30">
    <div class="container mx-auto max-w-6xl">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <div class="md:col-span-2">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-purple-600 flex items-center justify-center">
              <span class="text-white font-bold text-sm">B&P</span>
            </div>
            <span class="text-xl font-bold text-foreground">Bill&Pay</span>
          </div>
          <p class="text-muted-foreground text-sm mb-4">
            다단계 영업 구조의 복잡한 수수료 체계를 자동화하는<br />
            차세대 정산 플랫폼
          </p>
          <p class="text-muted-foreground text-sm">
            © 2024 Bill&Pay. All rights reserved.
          </p>
        </div>
        
        <div>
          <h4 class="font-semibold text-foreground mb-4">제품</h4>
          <ul class="space-y-2 text-sm text-muted-foreground">
            <li><a href="#features" class="hover:text-foreground transition-colors">기능</a></li>
            <li><a href="#pricing" class="hover:text-foreground transition-colors">가격</a></li>
            <li><span class="cursor-pointer hover:text-foreground transition-colors">API 문서</span></li>
            <li><span class="cursor-pointer hover:text-foreground transition-colors">릴리즈 노트</span></li>
          </ul>
        </div>
        
        <div>
          <h4 class="font-semibold text-foreground mb-4">고객지원</h4>
          <ul class="space-y-2 text-sm text-muted-foreground">
            <li><span class="cursor-pointer hover:text-foreground transition-colors">고객센터</span></li>
            <li><span class="cursor-pointer hover:text-foreground transition-colors">이용약관</span></li>
            <li><span class="cursor-pointer hover:text-foreground transition-colors">개인정보처리방침</span></li>
            <li><a href="mailto:support@billpay.kr" class="hover:text-foreground transition-colors">support@billpay.kr</a></li>
          </ul>
        </div>
      </div>
    </div>
  </footer>
</div>
