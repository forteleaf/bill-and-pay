# Bill&Pay 개발일정 로드맵

> 작성일: 2026-02-20
> 기간: 4주 (2026-02-24 ~ 2026-03-20)
> 목표: 운영 안정성 확보 및 프로덕션 배포 준비

---

## 1. 프로젝트 현황 요약

### 구현 완료 항목

| 영역 | 항목 | 수량 |
|------|------|------|
| **백엔드** | Controller | 25개 (API 17 + Platform 7 + Webhook 1) |
| | Service | 36개 |
| | Entity | 28개 |
| | Repository | 28개 |
| | DTO | 74개 (Request 28 + Response 41 + 기타 5) |
| | Enum | 28개 |
| | Flyway 마이그레이션 | 12개 (public 4 + tenant 8) |
| **프론트엔드** | 페이지/라우트 | 42개 |
| | 비즈니스 컴포넌트 | 20개 |
| | UI 컴포넌트 | 113개 (19개 카테고리) |
| | API 클라이언트 | 15개 |
| | 타입 정의 | 12개 |
| **인프라** | Docker Compose | 3개 (dev, prod, nixos) |
| | 환경 설정 | .env, .env.production, application-prod.yml |
| | 빌드 | Gradle + Bun 정상 빌드 확인 |

### 핵심 아키텍처 구현 완료

- 멀티테넌트 (Schema-per-Tenant, ScopedValue)
- ltree 기반 5단계 조직 계층 + 권한 제어
- 복식부기 정산 엔진 (Zero-Sum 검증, 부분취소 비례 계산)
- JWT 인증 (Access + Refresh Token)
- KORPAY 웹훅 처리 (HMAC-SHA256, 멱등성)
- 플랫폼 관리 (테넌트 생성, 공지사항, 감사 로그, 모니터링)

### 미구현 핵심 항목

| 우선순위 | 항목 | 상태 |
|----------|------|------|
| **Critical** | 테스트 (BE 2개, FE 0개) | ❌ |
| **High** | 2FA (TOTP) | ❌ (DB 스키마만 존재) |
| **High** | 모니터링 (Micrometer + Prometheus) | ❌ |
| **High** | 구조화 로깅 (JSON) | 🟡 기본만 |
| **Medium** | 개인정보 AES-256 암호화 | 🟡 부분 |
| **Medium** | 정산 확정/검증 배치 | ❌ |
| **Medium** | DB 파티션 자동 관리 | 🟡 초기화만 |
| **Low** | 백업/복구 자동화 | ❌ |
| **Low** | 로그 수집 (ELK) | ❌ |
| **Low** | 모니터링 대시보드 (Grafana) | ❌ |

---

## 2. 4주 상세 로드맵

```
Week 1 ─── 코드 안정화 & 테스트 인프라
  │
Week 2 ─── 핵심 테스트 작성 & 보안 강화 (2FA)
  │
Week 3 ─── 모니터링·로깅 & 배치 작업 구현
  │
Week 4 ─── 통합 테스트 & 운영 준비 & 문서화
```

---

## 3. 주차별 상세 일정

### Week 1: 코드 안정화 & 테스트 인프라 (02/24 ~ 02/28)

> 목표: 안정적인 테스트 환경 구축

| # | 작업 항목 | 영역 | 우선순위 | 산출물 |
|---|----------|------|----------|--------|
| 1-1 | 테스트 인프라 구축 (BE) | BE | Critical | build.gradle 테스트 설정, TestContainers 도입 |
| 1-2 | 테스트 인프라 구축 (FE) | FE | Critical | vitest 설정, testing-library/svelte 도입 |
| 1-3 | .gitignore 정비 | Infra | High | nohup.out, backend.log, .ruff_cache 제외 |
| 1-4 | 환경변수 보안 강화 | BE | High | 하드코딩된 시크릿 제거, 프로덕션 필수값 검증 |
| 1-5 | Testcontainers PostgreSQL 설정 | BE | High | 멀티테넌트 테스트용 DB 컨텍스트 |

#### 상세 설명

**1-1. 백엔드 테스트 인프라**
- JUnit 5 + Mockito 기본 설정 확인
- Testcontainers PostgreSQL 18 + ltree 확장 설정
- 테스트용 Flyway 마이그레이션 실행 확인
- `@SpringBootTest` 기본 컨텍스트 로딩 검증
- 테스트 프로파일 (`application-test.yml`) 작성

**1-2. 프론트엔드 테스트 인프라**
- vitest 의존성 추가 (`bun add -D vitest @testing-library/svelte`)
- vite.config.ts에 테스트 설정 추가
- 샘플 테스트 작성 (utils.ts 단위 테스트)

---

### Week 2: 핵심 테스트 & 보안 강화 (03/02 ~ 03/06)

> 목표: 핵심 비즈니스 로직 테스트 커버리지 확보, 2FA 구현

| # | 작업 항목 | 영역 | 우선순위 | 산출물 |
|---|----------|------|----------|--------|
| 2-1 | 정산 엔진 단위 테스트 | BE | Critical | FeeCalculation, ZeroSum, PartialCancel 테스트 |
| 2-2 | 웹훅 처리 단위 테스트 | BE | Critical | 서명 검증, 파싱, 멱등성 테스트 |
| 2-3 | 인증/권한 테스트 | BE | High | JWT 발급/갱신, ltree 권한 검증 |
| 2-4 | 2FA (TOTP) 백엔드 구현 | BE | High | TotpService, AuthController 2FA 엔드포인트 |
| 2-5 | 2FA (TOTP) 프론트엔드 구현 | FE | High | QR 설정 UI, OTP 입력 로그인 플로우 |
| 2-6 | API 클라이언트 테스트 (FE) | FE | Medium | 주요 API 호출 모킹 테스트 |

#### 상세 설명

**2-1. 정산 엔진 테스트 (최우선)**
```
대상 클래스:
- FeeCalculationService: 계층별 마진 계산 정확성
- ZeroSumValidator: |이벤트 금액| = SUM(정산 amount) 검증
- PartialCancelCalculator: 부분취소 비례 계산, 단수 차이 흡수
- SettlementCreationService: 이벤트 → 정산 생성 흐름
- DailySettlementService: 일별 정산 집계

테스트 시나리오:
- 5단계 계층 수수료 분배 (정상 케이스)
- 부분취소 (50%, 다중 부분취소)
- Zero-Sum 위반 시 예외 발생
- BigDecimal 단수 차이 처리
```

**2-4. 2FA (TOTP) 구현**
```
백엔드:
- TotpService (시크릿 생성, 코드 검증)
- /api/auth/2fa/setup (QR 코드 반환)
- /api/auth/2fa/verify (코드 검증 + JWT 발급)
- /api/auth/2fa/enable, /disable
- 총판(DISTRIBUTOR) 계정 필수 적용

프론트엔드:
- 2FA 설정 페이지 (QR 코드 표시, 백업 코드)
- 로그인 2단계 (비밀번호 → OTP 입력)
```

---

### Week 3: 모니터링·로깅 & 배치 작업 (03/09 ~ 03/13)

> 목표: 운영 가시성 확보, 정산 배치 자동화

| # | 작업 항목 | 영역 | 우선순위 | 산출물 |
|---|----------|------|----------|--------|
| 3-1 | Micrometer + Prometheus 설정 | BE | High | 의존성 추가, /actuator/prometheus 엔드포인트 |
| 3-2 | 커스텀 메트릭 추가 | BE | High | 정산 처리량, 웹훅 지연시간, API 응답시간 |
| 3-3 | 구조화 로깅 (Logback JSON) | BE | High | logback-spring.xml, RequestId 추적 |
| 3-4 | 정산 확정 배치 구현 | BE | Medium | PENDING → CONFIRMED 자동 전환 스케줄러 |
| 3-5 | 데이터 정합성 검증 배치 | BE | Medium | Zero-Sum 일일 검증, 실패 시 보류 + 알림 |
| 3-6 | DB 파티션 자동 관리 | BE | Medium | 향후 30일 파티션 자동 생성 스케줄러 |
| 3-7 | 프론트엔드 완성도 점검 | FE | Medium | UX 버그 수정, 에러 핸들링 개선 |

#### 상세 설명

**3-1. Micrometer 설정**
```gradle
// build.gradle 추가
implementation 'io.micrometer:micrometer-registry-prometheus'
```
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  metrics:
    tags:
      application: billpay
```

**3-2. 커스텀 메트릭**
```
수집 대상:
- billpay.settlement.created (Counter): 정산 생성 건수
- billpay.settlement.batch.duration (Timer): 배치 처리 시간
- billpay.webhook.processing.duration (Timer): 웹훅 처리 시간
- billpay.api.response.time (Timer): API 응답 시간
- billpay.tenant.request.count (Counter): 테넌트별 요청 수
```

**3-4. 정산 확정 배치**
```
실행 주기: 매일 00:00 KST
로직:
1. settlement_date <= CURRENT_DATE인 PENDING 상태 조회
2. 데이터 정합성 검증 통과 여부 확인
3. 상태 CONFIRMED로 일괄 변경
4. 결과 로그 기록
```

---

### Week 4: 통합 테스트 & 운영 준비 (03/16 ~ 03/20)

> 목표: E2E 검증, 프로덕션 배포 체크리스트 완료

| # | 작업 항목 | 영역 | 우선순위 | 산출물 |
|---|----------|------|----------|--------|
| 4-1 | 통합 테스트: 웹훅 → 정산 흐름 | BE | Critical | 웹훅 수신 → 거래 생성 → 정산 자동 생성 E2E |
| 4-2 | 통합 테스트: 멀티테넌트 격리 | BE | High | 테넌트 간 데이터 격리 검증 |
| 4-3 | 개인정보 암호화 적용 | BE | High | AES-256 암호화 서비스, 계좌번호·주민번호 대상 |
| 4-4 | 백업/복구 스크립트 | Infra | Medium | pg_dump 자동 백업, 테넌트별 복구 테스트 |
| 4-5 | 운영 배포 체크리스트 작성 | Infra | Medium | 배포 전 검증 항목 문서화 |
| 4-6 | API 문서 정비 | BE | Low | Swagger/OpenAPI 엔드포인트 최신화 |
| 4-7 | 프론트엔드 빌드 최적화 확인 | FE | Low | 번들 사이즈 검증, Lighthouse 점수 확인 |

#### 상세 설명

**4-1. 웹훅 → 정산 통합 테스트**
```
시나리오:
1. KORPAY 웹훅 수신 (승인)
   → Transaction 생성 확인
   → TransactionEvent 기록 확인
   → Settlement 자동 생성 확인 (계층별)
   → Zero-Sum 검증 통과 확인

2. KORPAY 웹훅 수신 (취소)
   → 역분개 Settlement 생성 확인
   → DEBIT 엔트리 검증

3. KORPAY 웹훅 수신 (부분취소)
   → 비례 계산 검증
   → 단수 차이 총판 흡수 확인
```

**4-3. 개인정보 암호화**
```
대상 필드:
- SettlementAccount.accountNumber (계좌번호)
- BusinessEntity.registrationNumber (사업자번호)
- PgConnection.credentials (API 키/Secret)

구현:
- EncryptionService (AES-256-GCM)
- @Encrypted 어노테이션 + JPA AttributeConverter
- 기존 데이터 마이그레이션 스크립트
```

---

## 4. 마일스톤 체크리스트

### M1: 테스트 기반 확보 (Week 1 완료 시점)
- [ ] 백엔드 테스트 실행 환경 (Testcontainers) 정상 동작
- [ ] 프론트엔드 테스트 실행 환경 (vitest) 정상 동작
- [ ] .gitignore에 민감 파일 제외 완료
- [ ] 하드코딩 시크릿 제거 완료

### M2: 핵심 로직 검증 & 보안 (Week 2 완료 시점)
- [ ] 정산 엔진 단위 테스트 커버리지 > 80%
- [ ] 웹훅 서명 검증 테스트 통과
- [ ] Zero-Sum 검증 실패 케이스 테스트 통과
- [ ] 2FA (TOTP) 로그인 플로우 정상 동작
- [ ] 총판 계정 2FA 필수 적용

### M3: 운영 가시성 확보 (Week 3 완료 시점)
- [ ] /actuator/prometheus 엔드포인트 정상 응답
- [ ] 커스텀 메트릭 5종 이상 수집 확인
- [ ] JSON 구조화 로깅 적용 (운영 프로파일)
- [ ] 정산 확정 배치 일일 자동 실행
- [ ] 데이터 정합성 검증 배치 정상 동작

### M4: 프로덕션 준비 완료 (Week 4 완료 시점)
- [ ] 웹훅 → 정산 E2E 통합 테스트 통과
- [ ] 멀티테넌트 격리 테스트 통과
- [ ] 개인정보 AES-256 암호화 적용 완료
- [ ] 백업/복구 스크립트 동작 검증
- [ ] 운영 배포 체크리스트 문서 완성
- [ ] 전체 백엔드 테스트 커버리지 > 50%

---

## 5. 리스크 관리

### 높은 리스크

| 리스크 | 영향 | 완화 방안 |
|--------|------|----------|
| Testcontainers + 멀티테넌트 환경 설정 복잡도 | Week 1 지연 | 단일 테넌트 테스트부터 시작, 점진적 확장 |
| 정산 엔진 테스트 시 예상치 못한 엣지 케이스 발견 | Week 2 지연 + 버그 수정 필요 | 핵심 경로만 우선 커버, 엣지 케이스는 이슈 등록 후 추후 처리 |
| 2FA 도입 시 기존 인증 플로우 변경 범위 | 로그인 장애 가능 | Feature Flag로 단계적 적용, 총판만 우선 |

### 중간 리스크

| 리스크 | 영향 | 완화 방안 |
|--------|------|----------|
| AES-256 암호화 적용 시 기존 데이터 마이그레이션 | 데이터 손실 가능 | 백업 후 마이그레이션, 롤백 스크립트 준비 |
| 정산 배치 대량 데이터 처리 성능 | 배치 타임아웃 | 청크 단위 처리, 배치 사이즈 조절 |

### 낮은 리스크

| 리스크 | 영향 | 완화 방안 |
|--------|------|----------|
| 프론트엔드 테스트 환경 Svelte 5 호환성 | FE 테스트 지연 | @testing-library/svelte 최신 버전 사용 |
| Prometheus/Grafana 리소스 부담 | 인프라 비용 증가 | 개발 환경에서는 비활성화, 운영만 적용 |

---

## 6. 4주 이후 로드맵 (향후 과제)

아래 항목은 4주 내 범위에서 제외하되, 이후 단계에서 진행합니다.

| 항목 | 우선순위 | 비고 |
|------|----------|------|
| Grafana 모니터링 대시보드 | High | Prometheus 연동 후 구축 |
| ELK Stack 로그 수집 | Medium | 운영 로그 검색/분석 |
| 분산 추적 (OpenTelemetry) | Medium | 멀티테넌트 요청 추적 |
| 정적 분석 (SonarQube) | Medium | 코드 품질 자동 점검 |
| Redis 세션 저장소 | Low | 다중 인스턴스 환경 대비 |
| 부하 테스트 (k6/JMeter) | Low | SLA 목표값 검증 |
| 프론트엔드 E2E 테스트 (Playwright) | Low | 주요 사용자 시나리오 자동화 |

---

## 부록: 현재 코드 통계

### 백엔드 구성 상세

```
src/main/java/com/korpay/billpay/
├── config/          # 설정 (Security, Tenant, CORS, Flyway)
├── controller/
│   ├── api/         # 17개 REST Controller
│   └── platform/    # 7개 Platform Controller + Webhook 1개
├── domain/
│   ├── entity/      # 28개 JPA Entity
│   └── enums/       # 28개 Enum
├── dto/
│   ├── request/     # 28개 Request DTO
│   ├── response/    # 41개 Response DTO
│   ├── settlement/  # 2개 정산 DTO
│   └── webhook/     # 3개 웹훅 DTO
├── exception/       # 예외 처리 (GlobalExceptionHandler)
├── repository/      # 28개 Repository
├── service/         # 36개 Service
└── util/            # 유틸리티

src/test/java/       # 2개 테스트 파일 (BcryptTest, PgConnectionControllerTest)
```

### 프론트엔드 구성 상세

```
frontend/src/
├── api/             # 15개 API 클라이언트
├── components/
│   ├── layout/      # 5개 레이아웃 컴포넌트
│   ├── organization/# 6개 조직 컴포넌트
│   ├── merchant/    # 2개 가맹점 컴포넌트
│   ├── settlement/  # 3개 정산 컴포넌트
│   ├── shared/      # 2개 공유 컴포넌트
│   └── platform/    # 2개 플랫폼 컴포넌트
├── lib/
│   └── components/ui/  # 113개 UI 컴포넌트 (19 카테고리)
├── routes/
│   ├── auth/        # 2개 (Login, Landing)
│   ├── dashboard/   # 1개
│   ├── transaction/ # 1개
│   ├── settlement/  # 8개
│   ├── merchant/    # 6개
│   ├── branch/      # 4개
│   ├── terminal/    # 2개
│   ├── user/        # 3개
│   ├── pg-connection/# 2개
│   ├── organization/# 1개
│   ├── platform/    # 9개
│   └── misc/        # 2개
├── stores/          # 상태 관리
└── types/           # 12개 타입 정의
```

### Flyway 마이그레이션

```
db/migration/
├── public/          # 4개 (V1~V4)
│   ├── V1__init_public_schema.sql
│   ├── V2__seed_data.sql
│   ├── V3__demo_requests_table.sql
│   └── V4__platform_admin_tables.sql
└── tenant/          # 8개 (V1~V8)
    ├── V1__init_tenant_schema.sql
    ├── V2__seed_data.sql
    ├── V3__seed_test_data.sql
    ├── V4__add_settlement_cycle.sql
    ├── V5__webhook_idempotency_and_unmapped.sql
    ├── V6__fee_configuration_history.sql
    ├── V7__add_merchant_corporate_fields.sql
    └── V8__add_settlement_account_unique_constraint.sql
```
