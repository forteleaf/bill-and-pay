# Bill&Pay - Finch/Docker 실행 가이드

## 사전 준비

### Finch 설치
```bash
brew install finch
finch vm init
finch vm start
```

### 환경 설정
프로젝트 루트에 `.env` 파일 생성:
```bash
cp .env.example .env
```

필요시 `.env` 파일 수정 (기본값 사용 가능):
```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=postgres
POSTGRES_PORT=5432

BACKEND_PORT=8080
FRONTEND_PORT=5173

SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xmx512m -Xms256m

VITE_API_BASE_URL=http://localhost:8080/api
```

## 실행 방법

### 1. 전체 스택 실행
```bash
finch compose up -d
```

### 2. 개별 서비스 실행
```bash
finch compose up -d postgres
finch compose up -d backend
finch compose up -d frontend
```

### 3. 로그 확인
```bash
finch compose logs -f
finch compose logs -f backend
```

### 4. 서비스 상태 확인
```bash
finch compose ps
```

## 접속 정보

| 서비스 | URL | 용도 |
|--------|-----|------|
| Frontend | http://localhost:5173 | Svelte UI |
| Backend API | http://localhost:8080/api | Spring Boot REST API |
| PostgreSQL | localhost:5432 | Database |

## 데이터베이스 초기화

컨테이너 첫 실행 시 자동으로:
1. PostgreSQL 18 설치
2. ltree 확장 설치
3. `billpay` 데이터베이스 생성
4. Flyway 마이그레이션 실행 (public + tenant_001 스키마)

### 수동 마이그레이션 (필요시)
```bash
finch compose exec backend ./gradlew flywayMigrate -Pflyway.schemas=tenant_002
```

## 서비스 종료

### 전체 종료
```bash
finch compose down
```

### 데이터 볼륨 포함 삭제
```bash
finch compose down -v
```

## 개발 모드

### 프론트엔드 Hot Reload
프론트엔드 컨테이너는 `frontend/src` 디렉토리를 마운트하여 자동 리로드 지원.

### 백엔드 재빌드
코드 변경 후:
```bash
finch compose up -d --build backend
```

## 트러블슈팅

### PostgreSQL 연결 실패
```bash
finch compose exec postgres pg_isready -U postgres
finch compose logs postgres
```

### 백엔드 실행 실패
```bash
finch compose logs backend
finch compose exec backend java -version
```

### 포트 충돌
`.env` 파일에서 `POSTGRES_PORT`, `BACKEND_PORT`, `FRONTEND_PORT` 변경.

### 데이터 초기화
```bash
finch compose down -v
finch volume prune
finch compose up -d
```

## 빌드 캐시 정리
```bash
finch builder prune
finch image prune -a
```

## 아키텍처

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│   Backend    │────▶│  PostgreSQL  │
│  (Svelte 5)  │     │(Spring Boot) │     │     18       │
│   :5173      │     │    :8080     │     │    :5432     │
└──────────────┘     └──────────────┘     └──────────────┘
      │                     │                     │
      └─────────────────────┴─────────────────────┘
                  billpay-network (bridge)
```

## 멀티테넌시 테스트

### 테넌트 스키마 생성
```bash
finch compose exec postgres psql -U postgres -d billpay -c "CREATE SCHEMA IF NOT EXISTS tenant_002;"
```

### API 요청 (X-Tenant-ID 헤더)
```bash
curl -H "X-Tenant-ID: tenant_001" http://localhost:8080/api/v1/organizations
```

## 운영 고려사항

### 리소스 제한 (compose.yaml 수정)
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### 백업
```bash
finch compose exec postgres pg_dump -U postgres billpay > backup.sql
```

### 복원
```bash
cat backup.sql | finch compose exec -T postgres psql -U postgres billpay
```
