# Bill&Pay Frontend

Svelte 5 + Vite 기반 정산 플랫폼 프론트엔드

## 기술 스택

- **Svelte 5** - Runes API ($state, $derived, $effect)
- **Vite** - 빌드 도구
- **TypeScript** - 타입 안정성
- **TanStack Table** - 데이터 테이블
- **Bits UI** - UI 컴포넌트

## 시작하기

### 1. 의존성 설치

```bash
cd frontend
npm install
```

### 2. 개발 서버 실행

```bash
npm run dev
```

브라우저에서 http://localhost:5173 접속

### 3. 빌드

```bash
npm run build
```

### 4. TEST

생성된 테스트 데이터 요약

| 테이블 | 건수 | 내용 |
|--------|------|------|
| business_entities | 12 | 사업자 정보 (법인 3, 개인 8, 비사업자 1) |
| organizations | 10 | 5단계 계층 (총판 1, 대리점 2, 딜러 2, 판매점 2, 가맹 3) |
| users | 7 | 테스트 사용자 (비밀번호: password123) |
| merchants | 3 | 가맹점 |
| merchant_pg_mappings | 3 | PG 매핑 |
| terminals | 4 | 단말기 (CAT, POS, KIOSK, ONLINE) |
| fee_configurations | 10 | 수수료 설정 (단계별) |
| contacts | 6 | 담당자 |
| settlement_accounts | 4 | 정산계좌 |
| transactions | 5 | 거래 (승인 3, 취소 1, 부분취소 1) |
| transaction_events | 7 | 이벤트 (승인 5, 취소 1, 부분취소 1) |
| settlements | 10 | 정산 (Zero-Sum 검증 완료) |

---
계층 구조

```
코르페이 총판 (DIST001) - dist_001
├── 서울대리점 (AGCY001) - dist_001.agcy_001
│   ├── 강남딜러 (DEAL001) - dist_001.agcy_001.deal_001
│   │   ├── 역삼판매점 (SELL001) - dist_001.agcy_001.deal_001.sell_001
│   │   │   ├── 맛있는 커피숍 (VEND001) - MCH001
│   │   │   └── 행복한 분식점 (VEND002) - MCH002
│   │   └── 삼성판매점 (SELL002) - dist_001.agcy_001.deal_001.sell_002
│   │       └── 프리미엄마트 (VEND003) - MCH003
│   └── 서초딜러 (DEAL002)
└── 부산대리점 (AGCY002)
```
---

테스트 계정

| 사용자 | 이메일 | 역할 |
|--------|--------|------|
| admin | admin@korpay.com | SUPER_ADMIN |
| dist_admin | master@korpay-dist.com | DISTRIBUTOR_ADMIN |
| seoul_admin | seoul@agency.com | AGENCY_ADMIN |
| dealer1 | gangnam@dealer.com | DEALER |
| seller1 | yeoksam@seller.com | SELLER |
| merchant1 | coffee@merchant.com | MERCHANT |
비밀번호: 모두 password123

---

사용법

앱 시작 시 자동으로 마이그레이션됩니다:

# Docker로 실행

docker compose up -d

# 또는 직접 실행

./gradlew bootRun


## 프로젝트 구조

```
frontend/
├── src/
│   ├── main.ts              # 엔트리 포인트
│   ├── App.svelte          # 루트 컴포넌트
│   ├── routes/             # 페이지
│   │   ├── Dashboard.svelte
│   │   ├── Transactions.svelte
│   │   └── Settlements.svelte
│   ├── components/         # 재사용 컴포넌트
│   │   ├── Layout.svelte
│   │   ├── Sidebar.svelte
│   │   └── Header.svelte
│   ├── lib/                # 유틸리티
│   │   ├── api.ts         # API 클라이언트
│   │   └── stores.ts      # Svelte stores
│   └── types/              # TypeScript 타입
│       └── api.ts
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## API 연동

백엔드 API는 `http://localhost:8080/api/v1`에서 실행되어야 합니다.

모든 API 요청에는 `X-Tenant-ID` 헤더가 필요합니다.

## 다음 단계

1. TanStack Table로 거래 내역 테이블 구현
2. 대시보드 차트 및 통계 구현
3. 정산 관리 화면 구현
4. JWT 인증 추가
5. 반응형 디자인 적용
