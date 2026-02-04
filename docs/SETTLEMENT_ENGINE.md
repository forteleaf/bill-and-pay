# Settlement Calculation Engine

Complete implementation of double-entry bookkeeping settlement engine with Zero-Sum validation and proportional partial cancellation logic per PRD-03.

## Architecture Overview

```
TransactionEvent (source of truth)
    ↓
SettlementService (orchestration layer)
    ↓
SettlementCreationService (routing by event type)
    ↓
FeeCalculationService (APPROVAL/CANCEL)
OR PartialCancelCalculator (PARTIAL_CANCEL)
    ↓
ZeroSumValidator (verification)
    ↓
SettlementRepository (persistence)
```

## Core Components

### 1. SettlementService
**Location**: `com.korpay.billpay.service.settlement.SettlementService`

Main orchestration service that:
- Loads merchant and payment method data
- Delegates to SettlementCreationService
- Provides @Transactional boundary for all-or-nothing settlement creation

**Key Methods**:
- `processTransactionEvent(TransactionEvent event)` - Main entry point
- `processTransactionEventWithMerchant(TransactionEvent, Merchant, String)` - With pre-loaded merchant

### 2. SettlementCreationService
**Location**: `com.korpay.billpay.service.settlement.SettlementCreationService`

Routes settlement creation based on event type:
- **APPROVAL** → FeeCalculationService (all CREDIT entries)
- **CANCEL** → FeeCalculationService (all DEBIT entries, full reversal)
- **PARTIAL_CANCEL** → PartialCancelCalculator (proportional DEBIT with rounding adjustment)

Enforces Zero-Sum validation before persistence.

### 3. FeeCalculationService
**Location**: `com.korpay.billpay.service.settlement.FeeCalculationService`

Calculates fee distribution for APPROVAL and CANCEL events:

**Calculation Logic**:
1. Calculate merchant fee: `amount × merchantFeeRate` (FLOOR rounding)
2. Merchant settlement: `amount - merchantFee`
3. For each ancestor organization (bottom-up):
   - Calculate margin: `(previousRate - orgRate) × amount`
   - Create settlement entry if margin > 0
4. Calculate master residual: `amount - SUM(all settlements)`
5. Assign residual to MASTER (DISTRIBUTOR)

**Key Features**:
- Uses BigDecimal for intermediate calculations
- Converts to Long (cents) with FLOOR rounding
- Logs detailed fee breakdown for debugging
- Maintains Zero-Sum by design (residual goes to MASTER)

### 4. PartialCancelCalculator
**Location**: `com.korpay.billpay.service.settlement.calculator.PartialCancelCalculator`

Handles proportional partial cancellation:

**Algorithm**:
1. Fetch original APPROVAL settlements
2. Calculate cancel ratio: `cancelAmount / originalAmount`
3. For each original settlement:
   - `cancelAmount = originalAmount × ratio` (FLOOR rounding)
   - Create DEBIT entry
4. Calculate rounding difference: `targetAmount - SUM(cancelAmounts)`
5. Adjust MASTER settlement to absorb difference
6. Validate final Zero-Sum

**Rounding Adjustment Example**:
```
Original: 100,000 KRW
Cancel: 33,333 KRW (33.333%)

Merchant:  97,000 × 0.33333 = 32,332.01 → 32,332 (FLOOR)
Vendor:       500 × 0.33333 =    166.66 →    166 (FLOOR)
Seller:       500 × 0.33333 =    166.66 →    166 (FLOOR)
Dealer:       500 × 0.33333 =    166.66 →    166 (FLOOR)
Agency:       500 × 0.33333 =    166.66 →    166 (FLOOR)
Master:       500 × 0.33333 =    166.66 →    166 (FLOOR)
                                         ──────────
                                          32,328

Difference: 33,333 - 32,328 = 5
Adjusted Master: 166 + 5 = 171 ✓

Final Total: 32,333 ✓ (Zero-Sum validated)
```

### 5. ZeroSumValidator
**Location**: `com.korpay.billpay.service.settlement.validator.ZeroSumValidator`

Validates Zero-Sum invariant:
```
|event.amount| == SUM(settlement.amount)
```

**Behavior**:
- Calculates absolute event amount
- Sums all settlement amounts (always positive)
- Throws `ZeroSumViolationException` if mismatch
- Logs detailed breakdown on failure

### 6. FeeConfigResolver
**Location**: `com.korpay.billpay.service.settlement.FeeConfigResolver`

Extracts fee rates from JSONB config:

**Config Structure**:
```json
{
  "feeRates": {
    "CREDIT_CARD": 0.03,
    "DEBIT_CARD": 0.015,
    "default": 0.025
  }
}
```

**Methods**:
- `resolveMerchantFeeRate(Merchant, paymentMethodCode)` → BigDecimal
- `resolveOrganizationFeeRate(Organization, paymentMethodCode)` → BigDecimal

Falls back to "default" if specific payment method not configured.

## Exception Hierarchy

### ZeroSumViolationException
Thrown when Zero-Sum validation fails.

**Contains**:
- transactionEventId
- eventAmount (signed)
- settlementTotal
- difference

### SettlementCalculationException
Base exception for all settlement calculation errors.

### FeeConfigNotFoundException
Thrown when fee configuration is missing for entity + payment method.

### OriginalSettlementNotFoundException
Thrown when partial cancel cannot find original approval settlements.

## DTO Classes

### FeeBreakdown
Debug DTO containing:
- entityId, entityType, entityPath
- feeRate, marginRate
- marginAmount, settlementAmount
- description

Used for logging and troubleshooting fee calculations.

### SettlementSummary
Aggregation DTO containing:
- entityId, entityType, entityPath
- entryType (CREDIT/DEBIT)
- totalAmount, feeAmount, netAmount
- settlementCount
- periodStart, periodEnd

Used for dashboard and reporting.

## Usage Examples

### Process Approval Event
```java
@Autowired
private SettlementService settlementService;

TransactionEvent approvalEvent = ...;
List<Settlement> settlements = settlementService.processTransactionEvent(approvalEvent);
// settlements contains: merchant CREDIT + hierarchy CREDITs + master CREDIT
```

### Process Cancel Event
```java
TransactionEvent cancelEvent = ...; // amount < 0
List<Settlement> settlements = settlementService.processTransactionEvent(cancelEvent);
// settlements contains: merchant DEBIT + hierarchy DEBITs + master DEBIT (full reversal)
```

### Process Partial Cancel Event
```java
TransactionEvent partialCancelEvent = ...; // amount < 0, partial
List<Settlement> settlements = settlementService.processTransactionEvent(partialCancelEvent);
// settlements contains: proportional DEBITs with rounding adjustment to MASTER
```

## Key Design Decisions

### 1. BigDecimal for Calculations
All fee calculations use `BigDecimal` with explicit scale and rounding mode:
- Prevents floating-point precision errors
- Ensures reproducible results
- Meets financial accuracy requirements

### 2. FLOOR Rounding Strategy
All monetary conversions use `RoundingMode.FLOOR`:
- Conservative approach (never overcharge)
- Consistent with PRD-03 specification
- MASTER absorbs all rounding differences

### 3. MASTER Residual Pattern
Unallocated amount always goes to MASTER (DISTRIBUTOR):
- Ensures Zero-Sum without complex adjustment logic
- Simplifies reconciliation
- MASTER has highest authority to absorb differences

### 4. Immutable Event Source
TransactionEvent is never modified:
- Settlement calculations are pure functions
- Enables audit trail and replay
- Supports event sourcing patterns

### 5. Single Transaction Boundary
All settlements for one event created in single transaction:
- Guarantees atomicity
- Simplifies error recovery
- Prevents partial settlement state

## Testing Recommendations

### Unit Tests
1. **FeeCalculationService**: Test fee distribution with various rates
2. **PartialCancelCalculator**: Test rounding adjustment logic
3. **ZeroSumValidator**: Test validation with edge cases
4. **FeeConfigResolver**: Test config parsing and fallback

### Integration Tests
1. **End-to-end approval**: Create event → verify settlements → check Zero-Sum
2. **End-to-end cancel**: Create approval → create cancel → verify full reversal
3. **End-to-end partial cancel**: Create approval → multiple partial cancels → verify cumulative
4. **Hierarchy variations**: Test with different org structures (2-level, 5-level)
5. **Rounding edge cases**: Test amounts that produce maximum rounding differences

### Example Test Case
```java
@Test
void testPartialCancelWithRounding() {
    // Approval: 100,000 KRW
    TransactionEvent approval = createApprovalEvent(100_000L);
    List<Settlement> approvalSettlements = service.processTransactionEvent(approval);
    assertZeroSum(approval, approvalSettlements);
    
    // Partial Cancel: 33,333 KRW (33.333%)
    TransactionEvent partialCancel = createPartialCancelEvent(approval, -33_333L);
    List<Settlement> cancelSettlements = service.processTransactionEvent(partialCancel);
    assertZeroSum(partialCancel, cancelSettlements);
    
    // Verify proportional distribution
    Settlement merchantCancel = findMerchantSettlement(cancelSettlements);
    assertEquals(32_332L, merchantCancel.getAmount()); // 97,000 × 0.33333 = 32,332
    
    // Verify MASTER absorbed rounding
    Settlement masterCancel = findMasterSettlement(cancelSettlements);
    assertTrue(masterCancel.getAmount() >= 166L); // May include rounding adjustment
}
```

## Monitoring and Alerting

### Key Metrics
1. **Zero-Sum violations**: Count (should be 0)
2. **Settlement creation latency**: p50, p95, p99
3. **Fee calculation errors**: Count by exception type
4. **Rounding adjustments**: Sum of absolute differences

### Log Levels
- **INFO**: Settlement creation start/complete, fee breakdowns
- **WARN**: Zero-Sum validation failures (before throw)
- **ERROR**: Unexpected calculation errors, missing configs
- **DEBUG**: Detailed calculation steps, intermediate values

## Maintenance Checklist

- [ ] Verify Zero-Sum in production daily
- [ ] Monitor rounding adjustment patterns
- [ ] Review fee config changes before deployment
- [ ] Test settlement calculations with production data samples
- [ ] Maintain audit trail of all settlement modifications
- [ ] Document any new payment method fee structures

## References

- [PRD-03: Ledger Design](docs/PRD-03_ledger.md)
- [Settlement Entity](src/main/java/com/korpay/billpay/domain/entity/Settlement.java)
- [TransactionEvent Entity](src/main/java/com/korpay/billpay/domain/entity/TransactionEvent.java)
