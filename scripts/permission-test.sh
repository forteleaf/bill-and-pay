#!/bin/bash
# =============================================================================
# Bill&Pay 권한 테스트 스크립트
# =============================================================================
# 7개 계정별 거래내역/정산내역/가맹점 조회 권한 테스트
# 사용법: bash scripts/permission-test.sh
# =============================================================================

set -euo pipefail

BASE_URL="http://localhost:8100/api"
PASSWORD="password123"
RESULTS_DIR="/tmp/billpay-permission-test"
rm -rf "$RESULTS_DIR"
mkdir -p "$RESULTS_DIR"

# ANSI 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# =============================================================================
# 테스트 사용자 정의
# =============================================================================
# 형식: username|role|orgPath|level|설명
USERS=(
  "admin|SUPER_ADMIN|dist_001|1|시스템 관리자 (총판)"
  "dist_admin|DISTRIBUTOR_ADMIN|dist_001|1|총판 관리자"
  "seoul_admin|AGENCY_ADMIN|dist_001.agcy_001|2|서울대리점 관리자"
  "busan_admin|AGENCY_ADMIN|dist_001.agcy_002|2|부산대리점 관리자"
  "dealer1|DEALER|dist_001.agcy_001.deal_001|3|강남딜러"
  "seller1|SELLER|dist_001.agcy_001.deal_001.sell_001|4|역삼셀러"
  "merchant1|MERCHANT|dist_001.agcy_001.deal_001.sell_001.vend_001|5|맛있는 커피숍"
)

# =============================================================================
# 시드 데이터 기준 예상 접근 권한 매트릭스
# =============================================================================
# 거래 데이터:
#   TXN1: 맛있는 커피숍 (vend_001) - path: dist_001.agcy_001.deal_001.sell_001.vend_001 - 15,000원
#   TXN2: 행복한 분식점 (vend_002) - path: dist_001.agcy_001.deal_001.sell_001.vend_002 - 8,500원
#   TXN3: 프리미엄마트 (vend_003) - path: dist_001.agcy_001.deal_001.sell_002.vend_003 - 125,000원
#
# 정산 데이터: TXN1에 대한 5개 정산 (VENDOR~DISTRIBUTOR 각 1건)
#   - VENDOR   (vend_001): entity_path = dist_001.agcy_001.deal_001.sell_001.vend_001
#   - SELLER   (sell_001): entity_path = dist_001.agcy_001.deal_001.sell_001
#   - DEALER   (deal_001): entity_path = dist_001.agcy_001.deal_001
#   - AGENCY   (agcy_001): entity_path = dist_001.agcy_001
#   - DISTRIBUTOR (dist_001): entity_path = dist_001
#
# 가맹점 데이터:
#   MCH001: 맛있는 커피숍 - path: dist_001.agcy_001.deal_001.sell_001.vend_001
#   MCH002: 행복한 분식점 - path: dist_001.agcy_001.deal_001.sell_001.vend_002
#   MCH003: 프리미엄마트  - path: dist_001.agcy_001.deal_001.sell_002.vend_003

cat << 'MATRIX'

╔══════════════════════════════════════════════════════════════════════════════╗
║                     예상 접근 권한 매트릭스 (EXPECTED)                       ║
╠══════════════════════════════════════════════════════════════════════════════╣
║                                                                            ║
║ 계층 구조:                                                                  ║
║ dist_001 (코르페이 총판)                                                    ║
║ ├── agcy_001 (서울대리점)                                                   ║
║ │   ├── deal_001 (강남딜러)                                                 ║
║ │   │   ├── sell_001 (역삼셀러)                                           ║
║ │   │   │   ├── vend_001 (맛있는 커피숍) ← MCH001, TXN1                   ║
║ │   │   │   └── vend_002 (행복한 분식점) ← MCH002, TXN2                   ║
║ │   │   └── sell_002 (삼성셀러)                                           ║
║ │   │       └── vend_003 (프리미엄마트) ← MCH003, TXN3                    ║
║ │   └── deal_002 (서초딜러)                                                 ║
║ └── agcy_002 (부산대리점)                                                   ║
║                                                                            ║
╠══════════════════════════════════════════════════════════════════════════════╣
║                                                                            ║
║ 거래내역 접근 (EXPECTED):                                                   ║
║ ┌─────────────┬──────┬──────┬──────┬─────────────────────────────────┐     ║
║ │ 사용자       │ TXN1 │ TXN2 │ TXN3 │ 사유                           │     ║
║ ├─────────────┼──────┼──────┼──────┼─────────────────────────────────┤     ║
║ │ admin       │  ✅  │  ✅  │  ✅  │ SUPER_ADMIN → 전체 접근         │     ║
║ │ dist_admin  │  ✅  │  ✅  │  ✅  │ dist_001 하위 전체              │     ║
║ │ seoul_admin │  ✅  │  ✅  │  ✅  │ agcy_001 하위 전체              │     ║
║ │ busan_admin │  ❌  │  ❌  │  ❌  │ agcy_002 하위 → 데이터 없음     │     ║
║ │ dealer1     │  ✅  │  ✅  │  ✅  │ deal_001 하위 전체              │     ║
║ │ seller1     │  ✅  │  ✅  │  ❌  │ sell_001 하위만 (TXN3은 sell_002)│    ║
║ │ merchant1   │  ✅  │  ❌  │  ❌  │ vend_001만                      │     ║
║ └─────────────┴──────┴──────┴──────┴─────────────────────────────────┘     ║
║                                                                            ║
║ 정산내역 접근 (EXPECTED - entity_path 기준):                                ║
║ ┌─────────────┬────┬────┬────┬────┬────┬───────────────────────────┐      ║
║ │ 사용자       │VEND│SELL│DEAL│AGCY│DIST│ 예상 조회 건수             │      ║
║ ├─────────────┼────┼────┼────┼────┼────┼───────────────────────────┤      ║
║ │ admin       │ ✅ │ ✅ │ ✅ │ ✅ │ ✅ │ 5건                        │      ║
║ │ dist_admin  │ ✅ │ ✅ │ ✅ │ ✅ │ ✅ │ 5건                        │      ║
║ │ seoul_admin │ ✅ │ ✅ │ ✅ │ ✅ │ ❌ │ 4건 (agcy_001 하위)        │      ║
║ │ busan_admin │ ❌ │ ❌ │ ❌ │ ❌ │ ❌ │ 0건 (agcy_002 하위 없음)   │      ║
║ │ dealer1     │ ✅ │ ✅ │ ✅ │ ❌ │ ❌ │ 3건 (deal_001 하위)        │      ║
║ │ seller1     │ ✅ │ ✅ │ ❌ │ ❌ │ ❌ │ 2건 (sell_001 하위)        │      ║
║ │ merchant1   │ ✅ │ ❌ │ ❌ │ ❌ │ ❌ │ 1건 (vend_001만)           │      ║
║ └─────────────┴────┴────┴────┴────┴────┴───────────────────────────┘      ║
║                                                                            ║
║ 가맹점 접근 (EXPECTED):                                                     ║
║ ┌─────────────┬──────┬──────┬──────┬─────────────────────────────────┐     ║
║ │ 사용자       │MCH01│MCH02│MCH03 │ 예상 조회 건수                   │     ║
║ ├─────────────┼──────┼──────┼──────┼─────────────────────────────────┤     ║
║ │ admin       │  ✅  │  ✅  │  ✅  │ 3건                             │     ║
║ │ dist_admin  │  ✅  │  ✅  │  ✅  │ 3건                             │     ║
║ │ seoul_admin │  ✅  │  ✅  │  ✅  │ 3건                             │     ║
║ │ busan_admin │  ❌  │  ❌  │  ❌  │ 0건                             │     ║
║ │ dealer1     │  ✅  │  ✅  │  ✅  │ 3건                             │     ║
║ │ seller1     │  ✅  │  ✅  │  ❌  │ 2건                             │     ║
║ │ merchant1   │  ✅  │  ❌  │  ❌  │ 1건                             │     ║
║ └─────────────┴──────┴──────┴──────┴─────────────────────────────────┘     ║
╚══════════════════════════════════════════════════════════════════════════════╝

MATRIX

# 예상 결과 정의 (username:expected_txn_count:expected_stl_count:expected_mch_count)
declare -A EXPECTED_TXN=( ["admin"]=3 ["dist_admin"]=3 ["seoul_admin"]=3 ["busan_admin"]=0 ["dealer1"]=3 ["seller1"]=2 ["merchant1"]=1 )
declare -A EXPECTED_STL=( ["admin"]=5 ["dist_admin"]=5 ["seoul_admin"]=4 ["busan_admin"]=0 ["dealer1"]=3 ["seller1"]=2 ["merchant1"]=1 )
declare -A EXPECTED_MCH=( ["admin"]=3 ["dist_admin"]=3 ["seoul_admin"]=3 ["busan_admin"]=0 ["dealer1"]=3 ["seller1"]=2 ["merchant1"]=1 )

# =============================================================================
# 유틸리티 함수
# =============================================================================
login() {
  local username=$1
  local result
  result=$(curl -s "$BASE_URL/v1/auth/login" \
    -X POST \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$PASSWORD\"}" 2>/dev/null)

  local token
  token=$(echo "$result" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null || echo "")

  if [ -z "$token" ]; then
    echo "LOGIN_FAILED"
  else
    echo "$token"
  fi
}

api_get() {
  local token=$1
  local endpoint=$2
  curl -s "$BASE_URL$endpoint" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: 001" 2>/dev/null
}

extract_count() {
  local json=$1
  local field=${2:-totalElements}
  echo "$json" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    if d.get('success'):
        data = d.get('data', {})
        # PagedResponse has totalElements
        if '$field' in data:
            print(data['$field'])
        # List response - count items
        elif isinstance(data, list):
            print(len(data))
        # If data has content array
        elif 'content' in data:
            print(len(data['content']))
        else:
            print('N/A')
    else:
        err = d.get('error', {})
        print('ERR:' + err.get('code', 'UNKNOWN'))
except Exception as e:
    print('PARSE_ERR')
" 2>/dev/null
}

# =============================================================================
# Phase 1: 병렬 로그인
# =============================================================================
echo -e "\n${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}  Phase 1: 병렬 로그인 (7개 계정)${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

declare -A TOKENS

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  (
    token=$(login "$username")
    echo "$token" > "$RESULTS_DIR/token_${username}"
    if [ "$token" = "LOGIN_FAILED" ]; then
      echo -e "  ${RED}✗${NC} $username ($desc) - 로그인 실패"
    else
      echo -e "  ${GREEN}✓${NC} $username ($desc) - 토큰 획득"
    fi
  ) &
done
wait

# 토큰 로드
for user_info in "${USERS[@]}"; do
  IFS='|' read -r username _ _ _ _ <<< "$user_info"
  TOKENS[$username]=$(cat "$RESULTS_DIR/token_${username}" 2>/dev/null || echo "LOGIN_FAILED")
done

echo ""

# =============================================================================
# Phase 2: 거래내역 조회 권한 테스트 (병렬)
# =============================================================================
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}  Phase 2: 거래내역 조회 권한 테스트 (GET /v1/transactions)${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  (
    token="${TOKENS[$username]}"
    if [ "$token" = "LOGIN_FAILED" ]; then
      echo "LOGIN_FAILED" > "$RESULTS_DIR/txn_${username}"
      exit 0
    fi
    result=$(api_get "$token" "/v1/transactions?size=100")
    echo "$result" > "$RESULTS_DIR/txn_raw_${username}.json"
    count=$(extract_count "$result")
    echo "$count" > "$RESULTS_DIR/txn_${username}"
  ) &
done
wait

echo -e "  ${BOLD}┌─────────────┬──────────┬──────────────────┬──────────┬──────────┐${NC}"
echo -e "  ${BOLD}│ 사용자       │ 역할      │ orgPath          │ 예상     │ 실제     │${NC}"
echo -e "  ${BOLD}├─────────────┼──────────┼──────────────────┼──────────┼──────────┤${NC}"

TXN_PASS=0
TXN_FAIL=0

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  actual=$(cat "$RESULTS_DIR/txn_${username}" 2>/dev/null || echo "ERR")
  expected=${EXPECTED_TXN[$username]}

  if [ "$actual" = "$expected" ]; then
    status="${GREEN}PASS${NC}"
    TXN_PASS=$((TXN_PASS + 1))
  else
    status="${RED}FAIL${NC}"
    TXN_FAIL=$((TXN_FAIL + 1))
  fi

  printf "  │ %-11s │ %-8s │ %-16s │ %4s건   │ %4s건   │ %b\n" \
    "$username" "$role" "$orgpath" "$expected" "$actual" "$status"
done

echo -e "  ${BOLD}└─────────────┴──────────┴──────────────────┴──────────┴──────────┘${NC}"
echo -e "  결과: ${GREEN}PASS ${TXN_PASS}${NC} / ${RED}FAIL ${TXN_FAIL}${NC}\n"

# =============================================================================
# Phase 3: 정산내역 조회 권한 테스트 (병렬)
# =============================================================================
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}  Phase 3: 정산내역 조회 권한 테스트 (GET /v1/settlements)${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  (
    token="${TOKENS[$username]}"
    if [ "$token" = "LOGIN_FAILED" ]; then
      echo "LOGIN_FAILED" > "$RESULTS_DIR/stl_${username}"
      exit 0
    fi
    result=$(api_get "$token" "/v1/settlements?size=100")
    echo "$result" > "$RESULTS_DIR/stl_raw_${username}.json"
    count=$(extract_count "$result")
    echo "$count" > "$RESULTS_DIR/stl_${username}"
  ) &
done
wait

echo -e "  ${BOLD}┌─────────────┬──────────┬──────────────────┬──────────┬──────────┐${NC}"
echo -e "  ${BOLD}│ 사용자       │ 역할      │ orgPath          │ 예상     │ 실제     │${NC}"
echo -e "  ${BOLD}├─────────────┼──────────┼──────────────────┼──────────┼──────────┤${NC}"

STL_PASS=0
STL_FAIL=0

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  actual=$(cat "$RESULTS_DIR/stl_${username}" 2>/dev/null || echo "ERR")
  expected=${EXPECTED_STL[$username]}

  if [ "$actual" = "$expected" ]; then
    status="${GREEN}PASS${NC}"
    STL_PASS=$((STL_PASS + 1))
  else
    status="${RED}FAIL${NC}"
    STL_FAIL=$((STL_FAIL + 1))
  fi

  printf "  │ %-11s │ %-8s │ %-16s │ %4s건   │ %4s건   │ %b\n" \
    "$username" "$role" "$orgpath" "$expected" "$actual" "$status"
done

echo -e "  ${BOLD}└─────────────┴──────────┴──────────────────┴──────────┴──────────┘${NC}"
echo -e "  결과: ${GREEN}PASS ${STL_PASS}${NC} / ${RED}FAIL ${STL_FAIL}${NC}\n"

# =============================================================================
# Phase 4: 가맹점 조회 권한 테스트 (병렬)
# =============================================================================
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}  Phase 4: 가맹점 조회 권한 테스트 (GET /v1/merchants)${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  (
    token="${TOKENS[$username]}"
    if [ "$token" = "LOGIN_FAILED" ]; then
      echo "LOGIN_FAILED" > "$RESULTS_DIR/mch_${username}"
      exit 0
    fi
    result=$(api_get "$token" "/v1/merchants?size=100")
    echo "$result" > "$RESULTS_DIR/mch_raw_${username}.json"
    count=$(extract_count "$result")
    echo "$count" > "$RESULTS_DIR/mch_${username}"
  ) &
done
wait

echo -e "  ${BOLD}┌─────────────┬──────────┬──────────────────┬──────────┬──────────┐${NC}"
echo -e "  ${BOLD}│ 사용자       │ 역할      │ orgPath          │ 예상     │ 실제     │${NC}"
echo -e "  ${BOLD}├─────────────┼──────────┼──────────────────┼──────────┼──────────┤${NC}"

MCH_PASS=0
MCH_FAIL=0

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  actual=$(cat "$RESULTS_DIR/mch_${username}" 2>/dev/null || echo "ERR")
  expected=${EXPECTED_MCH[$username]}

  if [ "$actual" = "$expected" ]; then
    status="${GREEN}PASS${NC}"
    MCH_PASS=$((MCH_PASS + 1))
  else
    status="${RED}FAIL${NC}"
    MCH_FAIL=$((MCH_FAIL + 1))
  fi

  printf "  │ %-11s │ %-8s │ %-16s │ %4s건   │ %4s건   │ %b\n" \
    "$username" "$role" "$orgpath" "$expected" "$actual" "$status"
done

echo -e "  ${BOLD}└─────────────┴──────────┴──────────────────┴──────────┴──────────┘${NC}"
echo -e "  결과: ${GREEN}PASS ${MCH_PASS}${NC} / ${RED}FAIL ${MCH_FAIL}${NC}\n"

# =============================================================================
# Phase 5: 개별 거래 상세 접근 테스트 (병렬)
# =============================================================================
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}  Phase 5: 개별 거래 상세 접근 테스트 (GET /v1/transactions/{id})${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

# TXN IDs from seed data
TXN1_ID="01960000-0000-7000-0009-000000000001"  # vend_001 (15,000)
TXN2_ID="01960000-0000-7000-0009-000000000002"  # vend_002 (8,500)
TXN3_ID="01960000-0000-7000-0009-000000000003"  # vend_003 (125,000)

# Expected: OK=접근가능, DENIED=접근불가
declare -A EXPECTED_TXN_DETAIL
EXPECTED_TXN_DETAIL["admin:TXN1"]="OK"
EXPECTED_TXN_DETAIL["admin:TXN2"]="OK"
EXPECTED_TXN_DETAIL["admin:TXN3"]="OK"
EXPECTED_TXN_DETAIL["dist_admin:TXN1"]="OK"
EXPECTED_TXN_DETAIL["dist_admin:TXN2"]="OK"
EXPECTED_TXN_DETAIL["dist_admin:TXN3"]="OK"
EXPECTED_TXN_DETAIL["seoul_admin:TXN1"]="OK"
EXPECTED_TXN_DETAIL["seoul_admin:TXN2"]="OK"
EXPECTED_TXN_DETAIL["seoul_admin:TXN3"]="OK"
EXPECTED_TXN_DETAIL["busan_admin:TXN1"]="DENIED"
EXPECTED_TXN_DETAIL["busan_admin:TXN2"]="DENIED"
EXPECTED_TXN_DETAIL["busan_admin:TXN3"]="DENIED"
EXPECTED_TXN_DETAIL["dealer1:TXN1"]="OK"
EXPECTED_TXN_DETAIL["dealer1:TXN2"]="OK"
EXPECTED_TXN_DETAIL["dealer1:TXN3"]="OK"
EXPECTED_TXN_DETAIL["seller1:TXN1"]="OK"
EXPECTED_TXN_DETAIL["seller1:TXN2"]="OK"
EXPECTED_TXN_DETAIL["seller1:TXN3"]="DENIED"
EXPECTED_TXN_DETAIL["merchant1:TXN1"]="OK"
EXPECTED_TXN_DETAIL["merchant1:TXN2"]="DENIED"
EXPECTED_TXN_DETAIL["merchant1:TXN3"]="DENIED"

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  (
    token="${TOKENS[$username]}"
    if [ "$token" = "LOGIN_FAILED" ]; then
      echo "LOGIN_FAILED" > "$RESULTS_DIR/txn_detail_${username}"
      exit 0
    fi

    results=""
    for txn_label_id in "TXN1:$TXN1_ID" "TXN2:$TXN2_ID" "TXN3:$TXN3_ID"; do
      IFS=':' read -r txn_label txn_id <<< "$txn_label_id"
      response=$(api_get "$token" "/v1/transactions/$txn_id")

      success=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print('OK' if d.get('success') else 'DENIED')" 2>/dev/null || echo "ERR")
      results="${results}${txn_label}=${success},"
    done

    echo "$results" > "$RESULTS_DIR/txn_detail_${username}"
  ) &
done
wait

echo -e "  ${BOLD}┌─────────────┬──────────┬────────────────────────────────────────────────────────────────┐${NC}"
echo -e "  ${BOLD}│ 사용자       │ orgPath  │ TXN1(vend_001)      TXN2(vend_002)      TXN3(vend_003)       │${NC}"
echo -e "  ${BOLD}├─────────────┼──────────┼────────────────────────────────────────────────────────────────┤${NC}"

DETAIL_PASS=0
DETAIL_FAIL=0

for user_info in "${USERS[@]}"; do
  IFS='|' read -r username role orgpath level desc <<< "$user_info"
  raw=$(cat "$RESULTS_DIR/txn_detail_${username}" 2>/dev/null || echo "ERR")

  line="  │ $(printf '%-11s' "$username") │ $(printf '%-8s' "$orgpath") │"

  for txn_label in TXN1 TXN2 TXN3; do
    actual=$(echo "$raw" | tr ',' '\n' | grep "^${txn_label}=" | cut -d= -f2)
    expected=${EXPECTED_TXN_DETAIL["$username:$txn_label"]}

    if [ "$actual" = "$expected" ]; then
      if [ "$actual" = "OK" ]; then
        line="$line ${GREEN}✅ OK${NC}    예상:OK     "
      else
        line="$line ${GREEN}🚫 DENIED${NC} 예상:DENIED "
      fi
      DETAIL_PASS=$((DETAIL_PASS + 1))
    else
      line="$line ${RED}✗ $actual${NC}  예상:$expected "
      DETAIL_FAIL=$((DETAIL_FAIL + 1))
    fi
  done

  echo -e "${line}│"
done

echo -e "  ${BOLD}└─────────────┴──────────┴────────────────────────────────────────────────────────────────┘${NC}"
echo -e "  결과: ${GREEN}PASS ${DETAIL_PASS}${NC} / ${RED}FAIL ${DETAIL_FAIL}${NC}\n"

# =============================================================================
# 최종 요약
# =============================================================================
TOTAL_PASS=$((TXN_PASS + STL_PASS + MCH_PASS + DETAIL_PASS))
TOTAL_FAIL=$((TXN_FAIL + STL_FAIL + MCH_FAIL + DETAIL_FAIL))
TOTAL=$((TOTAL_PASS + TOTAL_FAIL))

echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${CYAN}  최종 결과 요약${NC}"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
echo -e "  거래내역 목록:     ${GREEN}PASS ${TXN_PASS}${NC} / ${RED}FAIL ${TXN_FAIL}${NC} (총 7건)"
echo -e "  정산내역 목록:     ${GREEN}PASS ${STL_PASS}${NC} / ${RED}FAIL ${STL_FAIL}${NC} (총 7건)"
echo -e "  가맹점 목록:       ${GREEN}PASS ${MCH_PASS}${NC} / ${RED}FAIL ${MCH_FAIL}${NC} (총 7건)"
echo -e "  거래 상세 접근:    ${GREEN}PASS ${DETAIL_PASS}${NC} / ${RED}FAIL ${DETAIL_FAIL}${NC} (총 21건)"
echo -e ""
echo -e "  ${BOLD}전체: ${GREEN}PASS ${TOTAL_PASS}${NC} / ${RED}FAIL ${TOTAL_FAIL}${NC} (총 ${TOTAL}건)${NC}"

if [ $TOTAL_FAIL -gt 0 ]; then
  echo -e "\n  ${YELLOW}⚠ 실패 원인 분석:${NC}"
  echo -e "  ${YELLOW}  UserContextHolder.createFallbackUser()가 모든 사용자를 MASTER_ADMIN으로${NC}"
  echo -e "  ${YELLOW}  설정하여 ltree 기반 접근 제어가 우회되고 있습니다.${NC}"
  echo -e "  ${YELLOW}  → 파일: src/main/java/com/korpay/billpay/service/auth/UserContextHolder.java${NC}"
  echo -e "  ${YELLOW}  → 문제: createFallbackUser() 메서드가 항상 MASTER_ADMIN + DISTRIBUTOR 경로 사용${NC}"
fi

echo -e "\n${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  Raw 응답 데이터: ${RESULTS_DIR}/"
echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
