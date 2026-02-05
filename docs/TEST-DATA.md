테스트 데이터 생성 완료
생성된 데이터 현황
| 테이블 | 건수 | 설명 |
|--------|------|------|
| organizations | 10 | 5단계 계층 구조 (총판→대리점→딜러→판매점→가맹) |
| business_entities | 12 | 사업자 정보 (법인/개인/비사업자) |
| users | 7 | 테스트 사용자 (비밀번호: password123) |
| merchants | 3 | 가맹점 |
| terminals | 4 | 단말기 (CAT/POS/KIOSK/ONLINE) |
| transactions | 3 | 거래 (승인) |
| settlements | 5 | 정산 |
| fee_configurations | 10 | 수수료 설정 |
| contacts | 6 | 담당자 |
| settlement_accounts | 4 | 정산계좌 |
파일 위치
src/main/resources/db/migration/tenant/V3__seed_test_data.sql
사용 방법
1. DB 초기화 후 앱 시작 (권장):
      # DB 리셋
   docker exec postgres-postgres-1 psql -U postgres -d billpay -c "DROP SCHEMA tenant_001 CASCADE;"
   
   # 앱 시작 (자동 마이그레이션)
   ./gradlew bootRun
   
2. 기존 DB에 수동 적용:
      # Flyway CLI 또는 앱 재시작
   ./gradlew flywayMigrate -Pflyway.schemas=tenant_001
   
테스트 계정
| 사용자 | 이메일 | 역할 | 비밀번호 |
|--------|--------|------|----------|
| admin | admin@korpay.com | SUPER_ADMIN | password123 |
| dist_admin | master@korpay-dist.com | DISTRIBUTOR_ADMIN | password123 |
| seoul_admin | seoul@agency.com | AGENCY_ADMIN | password123 |
참고
- public.users의 testuser 비밀번호도 password123으로 업데이트됨
- 거래 데이터는 현재 날짜 기준 2시간 전 ~ 30분 전으로 생성됨
- settlements는 Transaction 1에 대한 5개 계층 정산 레코드 포함
