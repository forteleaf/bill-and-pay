# Bill&Pay 정산 플랫폼

다단계 영업 구조에서 발생하는 복잡한 수수료 체계를 자동화하고, 외부 PG사로부터 수신한 대량 결제 데이터를 원장(Ledger) 기반으로 정확하게 정산하는 플랫폼입니다.

## 기술 스택

| 영역 | 기술 | 버전 |
|------|------|------|
| Database | PostgreSQL | 18 |
| Backend | Spring Boot | 3.5.10 |
| Frontend | Svelte + shadcn-svelte | 5 |
| Runtime | Java | 25 LTS |
| Package Manager | Bun | 1.x |

## 핵심 아키텍처

### 복식부기 정산
- 모든 정산은 `transaction_events` 기준으로 생성
- **Zero-Sum 원칙**: `|이벤트 금액| = SUM(정산 amount)`

### ltree 계층 구조
```
5단계 계층: DISTRIBUTOR > AGENCY > DEALER > SELLER > VENDOR
경로 예시: dist_001.agcy_001.deal_001.sell_001.vend_001
```

### 멀티테넌시
- Schema-per-Tenant 방식
- `public`: 공통 데이터 (tenants, pg_connections, holidays)
- `tenant_xxx`: 테넌트별 데이터

## 로컬 개발 환경

### 사전 요구사항
- Docker & Docker Compose
- Bun (npm 대신 사용)

### 실행

```bash
# 환경 변수 설정
cp .env.example .env

# Docker Compose 실행
docker compose up -d

# 접속
# Frontend: http://localhost:5173
# Backend API: http://localhost:8100/api
# PostgreSQL: localhost:5432
```

## NixOS 배포

NixOS 서버에 배포 시 기존 PostgreSQL과 nginx를 활용합니다.

### 아키텍처

```
인터넷 → NixOS nginx (SSL) → Docker 컨테이너
                bnp.cardbin.net
                       │
          ┌────────────┴────────────┐
          │ /api/*                  │ /*
          ▼                         ▼
   billpay-backend:8100      billpay-frontend:3010
          │
          ▼
   NixOS PostgreSQL:5432 (billpay DB)
```

### 배포 순서

#### 1. DB 초기화

```bash
sudo -u postgres psql < scripts/init-nixos-db.sql
```

#### 2. NixOS nginx 설정

`nginx-billpay.nix`를 nix-config에 import 후:

```bash
nixos-rebuild switch
```

#### 3. Docker 컨테이너 실행

```bash
docker compose -f compose.nixos.yaml --env-file .env.production up -d --build
```

### 검증

```bash
# 헬스체크
curl https://bnp.cardbin.net/api/actuator/health

# 프론트엔드
curl -I https://bnp.cardbin.net/

# DB 연결
sudo -u postgres psql -d billpay -c "\dt"
```

## 프로젝트 구조

```
bill-and-pay/
├── src/main/
│   ├── java/com/korpay/billpay/    # Spring Boot 백엔드
│   └── resources/
│       ├── application.yml
│       └── db/migration/           # Flyway 마이그레이션
├── frontend/                       # Svelte 5 프론트엔드
├── docs/                           # PRD 문서
├── scripts/                        # 배포 스크립트
├── compose.yaml                    # 로컬 개발용
└── compose.nixos.yaml              # NixOS 배포용
```

## 문서

- [PRD-01: 아키텍처](docs/PRD-01_architecture.md)
- [PRD-02: 조직 구조](docs/PRD-02_organization.md)
- [PRD-03: 원장/정산](docs/PRD-03_ledger.md)
- [PRD-04: PG 연동](docs/PRD-04_pg_integration.md)
- [PRD-05: DB 스키마](docs/PRD-05_database_schema.md)
- [PRD-06: KORPAY](docs/PRD-06_korpay.md)
- [PRD-07: UI 화면 설계](docs/PRD-07_ui_screens.md)

## 라이선스

Private - All rights reserved
