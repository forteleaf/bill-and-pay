# JWT 인증 테스트 가이드

## 구현 완료 항목

### 백엔드 (Spring Boot)
- ✅ JWT 토큰 생성/검증 (JwtTokenProvider)
- ✅ Spring Security 필터 체인 (SecurityConfig)
- ✅ 인증 필터 (JwtAuthenticationFilter)
- ✅ UserDetailsService 구현 (CustomUserDetailsService)
- ✅ 인증 서비스 (AuthService)
- ✅ 인증 컨트롤러 (AuthController)
  - POST `/api/v1/auth/login` - 로그인
  - POST `/api/v1/auth/refresh` - 토큰 리프레시
  - POST `/api/v1/auth/logout` - 로그아웃

### 프론트엔드 (Svelte 5)
- ✅ Auth Store (authStore.ts)
  - localStorage 기반 토큰 관리
  - 로그인/로그아웃 상태 관리
- ✅ API 클라이언트 (api.ts)
  - JWT 토큰 자동 포함
  - 401 응답 시 자동 토큰 리프레시
  - 리프레시 실패 시 로그인 페이지로 리다이렉트
- ✅ 로그인 화면 (Login.svelte)
  - 사용자명/비밀번호 입력
  - 로그인 API 호출
  - 에러 처리 및 로딩 상태
- ✅ 로그아웃 기능 (Header.svelte)
  - 사용자 정보 표시
  - 로그아웃 버튼
- ✅ 인증 가드 (App.svelte)
  - 미인증 시 로그인 페이지로 리다이렉트

## 테스트 시나리오

### 1. 환경 준비

```bash
# 데이터베이스 시작
docker compose up -d postgres

# 백엔드 시작
./gradlew bootRun

# 프론트엔드 시작 (별도 터미널)
cd frontend
bun run dev
```

### 2. 테스트 사용자 생성

**Option 1: SQL로 직접 생성**
```sql
-- PostgreSQL 접속
psql -h localhost -U postgres -d billpay

-- public.users 테이블에 테스트 사용자 추가
INSERT INTO users (id, username, password, tenant_id, status, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'testuser',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- "password"
  'tenant_001',
  'ACTIVE',
  NOW(),
  NOW()
);
```

**Option 2: 백엔드 API로 생성 (구현 필요)**
```bash
# 사용자 생성 API는 아직 구현되지 않음
# 추후 관리자 기능으로 추가 예정
```

### 3. 로그인 테스트

#### 3.1 프론트엔드에서 테스트
1. 브라우저에서 `http://localhost:5173` 접속
2. 로그인 화면이 표시되는지 확인
3. 테스트 사용자로 로그인:
   - 사용자명: `testuser`
   - 비밀번호: `password`
4. 로그인 성공 후 대시보드로 이동 확인
5. 헤더에 사용자 정보 표시 확인

#### 3.2 API 직접 테스트
```bash
# 로그인 요청
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }'

# 응답 예시
# {
#   "success": true,
#   "data": {
#     "accessToken": "eyJhbGc...",
#     "refreshToken": "eyJhbGc...",
#     "username": "testuser",
#     "tenantId": "tenant_001"
#   }
# }
```

### 4. 인증된 API 요청 테스트

```bash
# 토큰 저장
ACCESS_TOKEN="eyJhbGc..."

# 대시보드 메트릭 조회
curl -X GET http://localhost:8080/api/v1/dashboard/metrics \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: tenant_001"

# 거래 내역 조회
curl -X GET "http://localhost:8080/api/v1/transactions?page=0&size=20" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: tenant_001"
```

### 5. 토큰 리프레시 테스트

```bash
# 토큰 리프레시 요청
REFRESH_TOKEN="eyJhbGc..."

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"

# 응답: 새로운 accessToken과 refreshToken 반환
```

### 6. 로그아웃 테스트

#### 6.1 프론트엔드에서 테스트
1. 헤더의 로그아웃 버튼 클릭
2. 확인 대화상자에서 확인 클릭
3. 로그인 페이지로 리다이렉트 확인
4. localStorage에서 토큰 삭제 확인 (개발자 도구)

#### 6.2 API 직접 테스트
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 7. 401 에러 처리 테스트

#### 7.1 만료된 토큰 테스트
1. `application.yml`에서 `jwt.expiration`을 짧게 설정 (예: 60000 = 1분)
2. 로그인 후 1분 대기
3. API 요청 시 자동으로 토큰 리프레시 시도 확인
4. 리프레시 성공 시 원래 요청 재시도 확인

#### 7.2 잘못된 토큰 테스트
```bash
# 잘못된 토큰으로 요청
curl -X GET http://localhost:8080/api/v1/dashboard/metrics \
  -H "Authorization: Bearer invalid_token" \
  -H "X-Tenant-ID: tenant_001"

# 응답: 401 Unauthorized
```

### 8. 브라우저 개발자 도구 확인

#### localStorage 확인
```javascript
// 콘솔에서 실행
localStorage.getItem('accessToken')
localStorage.getItem('refreshToken')
localStorage.getItem('username')
localStorage.getItem('tenantId')
```

#### Network 탭 확인
- 모든 API 요청에 `Authorization: Bearer ...` 헤더 포함 확인
- 401 응답 시 `/auth/refresh` 요청 자동 발생 확인
- 리프레시 후 원래 요청 재시도 확인

## 예상 문제 및 해결

### 1. 사용자를 찾을 수 없음
**증상**: `User not found with username: testuser`

**원인**: users 테이블에 테스트 사용자가 없음

**해결**:
```sql
-- public.users 테이블 확인
SELECT * FROM users;

-- 없으면 위의 SQL로 생성
```

### 2. 비밀번호 불일치
**증상**: 로그인 실패, 401 응답

**원인**: 입력한 비밀번호가 암호화된 값과 일치하지 않음

**해결**:
```java
// BCrypt로 비밀번호 해시 생성
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode("password");
System.out.println(hashedPassword);
// SQL에서 이 값 사용
```

### 3. CORS 에러
**증상**: 브라우저 콘솔에 CORS 에러

**원인**: SecurityConfig의 CORS 설정 누락 또는 잘못됨

**해결**: `SecurityConfig.java`의 CORS 설정 확인
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

### 4. 토큰 리프레시 무한 루프
**증상**: `/auth/refresh` 요청이 반복됨

**원인**: 리프레시 토큰도 만료됨

**해결**: 로그아웃 후 재로그인 필요

## 보안 체크리스트

- ✅ 비밀번호는 BCrypt로 암호화되어 저장
- ✅ JWT 시크릿은 환경변수로 관리 (`.env`)
- ✅ Access Token 유효기간: 24시간
- ✅ Refresh Token 유효기간: 7일
- ✅ HTTPS 사용 권장 (프로덕션)
- ⏳ 2FA 구현 (추후)
- ⏳ 토큰 블랙리스트 (추후)
- ⏳ Rate Limiting (추후)

## 다음 단계

1. **Seed 데이터 생성**
   - Flyway 마이그레이션으로 테스트 사용자 자동 생성
   - 개발 환경용 샘플 데이터 추가

2. **통합 테스트**
   - Spring Security Test로 인증 플로우 테스트
   - MockMvc로 API 엔드포인트 테스트

3. **UI 개선**
   - 비밀번호 찾기 기능
   - "로그인 유지" 체크박스
   - 세션 만료 알림

4. **보안 강화**
   - 2FA (TOTP) 구현
   - 로그인 시도 제한
   - IP 기반 접근 제어
   - 감사 로그 (audit log)

5. **사용자 관리**
   - 사용자 생성/수정/삭제 API
   - 권한 관리 (RBAC)
   - 비밀번호 정책 강제

## 참고 문서

- [CLAUDE.md](CLAUDE.md) - 프로젝트 개요
- [API_INTEGRATION_GUIDE.md](API_INTEGRATION_GUIDE.md) - API 연동 가이드
- [API_ENDPOINTS.md](API_ENDPOINTS.md) - API 명세
- [DOCKER_GUIDE.md](DOCKER_GUIDE.md) - Docker 실행 가이드
