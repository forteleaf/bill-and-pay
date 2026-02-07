#!/bin/bash
# =============================================================================
# 재정산 테스트 데이터 생성 실행 스크립트
# =============================================================================
# 사용법: ./scripts/run-seed-resettlement.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/seed-resettlement-test-data.sql"

echo "🔄 재정산 테스트 데이터 생성 중..."
echo ""

docker exec -i postgres-18 psql -U postgres -d billpay < "$SQL_FILE"

echo ""
echo "✅ 완료!"
