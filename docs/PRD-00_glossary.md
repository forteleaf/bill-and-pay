# PRD-00: 용어 사전 (Glossary)

## 1. 개요
Bill&Pay PRD 문서 전반에서 사용되는 용어를 표준화합니다. 모든 PRD 문서는 이 용어 사전의 정의를 따릅니다.

## 관련 문서
- [PRD-01: 아키텍처](PRD-01_architecture.md)
- [PRD-02: 조직 구조](PRD-02_organization.md)
- [PRD-03: 원장/정산](PRD-03_ledger.md)
- [PRD-04: PG 연동](PRD-04_pg_integration.md)
- [PRD-05: DB 스키마](PRD-05_database_schema.md)
- [PRD-06: KORPAY](PRD-06_korpay.md)
- [PRD-07: UI 화면 설계](PRD-07_ui_screens.md)

## 2. 조직 계층 용어

| 한글 | 영문(코드) | 약어 | Depth | 설명 |
|------|-----------|------|-------|------|
| 총판 | MASTER | - | 0 | 플랫폼 운영자, 테넌트 소유자 |
| 대리점 | DISTRIBUTOR | DI | 1 | 영업 최상위 계층 |
| 에이전시 | AGENCY | AG | 2 | 중간 영업 조직 |
| 딜러 | DEALER | DE | 3 | 중간 영업 조직 |
| 셀러 | SELLER | SE | 4 | 말단 영업 조직 |
| 벤더 | VENDOR | VE | 5 | 최하위 영업 조직 |
| 가맹점 | MERCHANT | - | - | 실제 거래 발생 지점 |

## 3. 거래/정산 용어

| 용어 | 영문 | 설명 |
|------|------|------|
| 거래 | Transaction | 결제/취소 등의 금융 행위 |
| 거래 이벤트 | Transaction Event | 거래의 개별 이력 (승인/취소/부분취소) |
| 정산 | Settlement | 거래에 대한 수수료 분배 기록 |
| 정산 배치 | Settlement Batch | 일별로 묶은 정산 그룹 |
| 복식부기 | Double-Entry | 모든 정산은 CREDIT/DEBIT 쌍으로 기록 |
| Zero-Sum | Zero-Sum | \|이벤트 금액\| = SUM(정산 amount) 원칙 |
| 역분개 | Reversal Entry | 취소 시 원정산의 반대 entry 생성 |
| 원장 | Ledger | 거래/정산 기록의 총체 |

## 4. 거래 상태

| 상태 | 코드 | 설명 |
|------|------|------|
| 승인 | APPROVED | 결제 승인 완료 |
| 전액취소 | CANCELLED | 전체 금액 취소 |
| 부분취소 | PARTIAL_CANCELLED | 일부 금액 취소 |
| 실패 | FAILED | 처리 실패 |
| 대기 | PENDING | 처리 대기 중 |

## 5. 정산 상태

| 상태 | 코드 | 설명 |
|------|------|------|
| 대기 | PENDING | 정산 예정일 미도래 |
| 처리중 | PROCESSING | 정산 처리 중 |
| 완료 | COMPLETED | 정산 처리 완료 |
| 실패 | FAILED | 정산 처리 실패 |
| 취소 | CANCELLED | 정산 취소 |

## 6. Entry Type

| 타입 | 코드 | 부호 | 발생 시점 |
|------|------|------|----------|
| 크레딧 | CREDIT | + (양수) | 승인 시 |
| 데빗 | DEBIT | - (음수) | 취소 시 |

## 7. 이벤트 유형

| 유형 | 코드 | amount 부호 | 설명 |
|------|------|------------|------|
| 승인 | APPROVAL | + (양수) | 최초 결제 승인 |
| 전액취소 | CANCEL | - (음수) | 전액 취소 |
| 부분취소 | PARTIAL_CANCEL | - (음수) | 부분 금액 취소 |
| 환불 | REFUND | - (음수) | 환불 처리 |

## 8. 기술 용어

| 용어 | 설명 |
|------|------|
| ltree | PostgreSQL 계층 데이터 관리 확장 타입. 조직 경로를 'dist_001.agcy_001' 형식으로 저장 |
| GiST Index | ltree 경로 검색을 위한 인덱스 타입 |
| JSONB | PostgreSQL의 바이너리 JSON 타입. 유연한 데이터 저장에 사용 |
| 파티셔닝 | transaction_events를 일별 RANGE 파티션으로 분할. 조회 성능 최적화 |
| Webhook | PG사가 Bill&Pay로 결제 이벤트를 실시간 POST하는 콜백 메커니즘 |
| HMAC-SHA256 | Webhook 서명 검증에 사용하는 해시 기반 메시지 인증 코드 |
| ScopedValue | Java Virtual Thread 기반 테넌트 컨텍스트 전파 메커니즘 |
| Schema-per-Tenant | 총판별 DB 스키마를 분리하는 멀티테넌시 방식 |
| Flyway | 데이터베이스 스키마 마이그레이션 도구 |
| D+N 정산 | 거래 발생일(D) 기준 N영업일 후 정산 (holidays 테이블 참조) |
| PgBouncer | PostgreSQL 커넥션 풀러. 멀티테넌트 환경에서 커넥션 효율화 |

## 9. 필드명 표준

| 개념 | 표준 필드명 | 설명 |
|------|-----------|------|
| 내부 거래 식별자 | transaction_id | Bill&Pay 내부 거래 ID (UUID) |
| PG 거래 식별자 | pg_transaction_id | PG사에서 부여한 거래 고유번호 |
| 거래 금액 | amount | 현재 거래 금액 (BIGINT) |
| 이벤트 발생 시점 | occurred_at | 실제 이벤트가 발생한 시간 |
| 정산 상태 | status | 정산의 현재 상태 (VARCHAR) |
| 조직 경로 | path / org_path | ltree 형식의 조직 계층 경로 |

## 변경 이력
| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-02-07 | 초안 작성 |
