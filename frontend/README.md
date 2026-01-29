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
