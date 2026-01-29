/**
 * Settlement calculation engine with double-entry bookkeeping.
 * 
 * <h2>Core Principles</h2>
 * <ul>
 *   <li><b>Double-Entry Bookkeeping</b>: Every transaction event creates balanced settlement entries</li>
 *   <li><b>Zero-Sum Invariant</b>: |event amount| = SUM(settlement amounts)</li>
 *   <li><b>CREDIT/DEBIT Logic</b>:
 *     <ul>
 *       <li>APPROVAL (+amount) → All entities get CREDIT entries</li>
 *       <li>CANCEL (-amount) → All entities get DEBIT entries (reverse posting)</li>
 *       <li>PARTIAL_CANCEL → Proportional DEBIT with MASTER absorbing rounding differences</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h2>Settlement Flow</h2>
 * <pre>
 * TransactionEvent (source)
 *   ↓
 * SettlementService (orchestration)
 *   ↓
 * SettlementCreationService (routing by event type)
 *   ↓
 * FeeCalculationService (APPROVAL/CANCEL)
 * OR PartialCancelCalculator (PARTIAL_CANCEL)
 *   ↓
 * ZeroSumValidator (verification)
 *   ↓
 * SettlementRepository (persistence)
 * </pre>
 * 
 * <h2>Fee Calculation Example</h2>
 * <pre>
 * Transaction: 100,000 KRW approval
 * Merchant fee rate: 3.0%
 * 
 * Merchant settlement:  97,000 KRW (CREDIT)
 * Vendor margin:           500 KRW (CREDIT) - 0.5% margin
 * Seller margin:           500 KRW (CREDIT) - 0.5% margin
 * Dealer margin:           500 KRW (CREDIT) - 0.5% margin
 * Agency margin:           500 KRW (CREDIT) - 0.5% margin
 * Master residual:         500 KRW (CREDIT) - remaining
 * ─────────────────────────────────
 * Total:              100,000 KRW ✓ (Zero-Sum verified)
 * </pre>
 * 
 * <h2>Partial Cancel Example</h2>
 * <pre>
 * Original: 100,000 KRW approval
 * Cancel: 30,000 KRW (30%)
 * 
 * Each settlement proportionally reversed:
 * Merchant: 97,000 × 30% = 29,100 KRW (DEBIT)
 * Vendor:      500 × 30% =    150 KRW (DEBIT)
 * ...
 * Master absorbs rounding difference to maintain Zero-Sum
 * </pre>
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link SettlementService} - Main entry point for settlement processing</li>
 *   <li>{@link SettlementCreationService} - Creates settlements based on event type</li>
 *   <li>{@link FeeCalculationService} - Calculates fee distribution across hierarchy</li>
 *   <li>{@link FeeConfigResolver} - Extracts fee rates from JSONB config</li>
 *   <li>{@link com.korpay.billpay.service.settlement.calculator.PartialCancelCalculator} - Proportional cancellation logic</li>
 *   <li>{@link com.korpay.billpay.service.settlement.validator.ZeroSumValidator} - Zero-Sum validation</li>
 * </ul>
 * 
 * @see com.korpay.billpay.domain.entity.Settlement
 * @see com.korpay.billpay.domain.entity.TransactionEvent
 */
package com.korpay.billpay.service.settlement;
