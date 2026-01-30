-- =============================================================================
-- Bill&Pay Tenant Schema - Fee Configuration Seed Data
-- =============================================================================
-- Description: Populates fee configuration data in merchants and organizations
-- 
-- Fee Structure (per PRD-03):
-- - SELLER fee: 3.6% (merchant keeps 96.4%)
-- - DEALER margin: 0.6%
-- - AGENCY margin: 1.0%
-- - DISTRIBUTOR margin: 2.0%
--
-- Payment Method Fees:
-- - CARD: 3.6%
-- - BANK_TRANSFER: 2.5%
-- - VIRTUAL_ACCOUNT: 2.5%
--
-- Zero-Sum Principle:
-- All settlements must satisfy: |transaction_amount| = SUM(settlement_amounts)
-- =============================================================================

-- =============================================================================
-- Update merchants with payment method fee rates
-- =============================================================================
-- Adds feeRates to merchants.config JSONB column
-- Preserves existing config data using COALESCE and jsonb merge operator (||)
-- =============================================================================
UPDATE merchants 
SET config = COALESCE(config, '{}'::jsonb) || jsonb_build_object(
  'feeRates', jsonb_build_object(
    'default', 0.036,
    'CARD', 0.036,
    'BANK_TRANSFER', 0.025,
    'VIRTUAL_ACCOUNT', 0.025
  )
)
WHERE status = 'ACTIVE';

COMMENT ON COLUMN merchants.config IS 'JSON: {feeRates: {default, CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT}, operating_hours, categories, features}';

-- =============================================================================
-- Update organizations with margin rates by type
-- =============================================================================
-- Adds feeRates to organizations.config JSONB column
-- Margin rates vary by organization type (DISTRIBUTOR > AGENCY > DEALER > SELLER)
-- SELLER has 0% margin (all fees go to SELLER level)
-- =============================================================================
UPDATE organizations 
SET config = COALESCE(config, '{}'::jsonb) || jsonb_build_object(
  'feeRates', jsonb_build_object(
    'marginRate', CASE org_type
      WHEN 'DISTRIBUTOR' THEN 0.020
      WHEN 'AGENCY' THEN 0.010
      WHEN 'DEALER' THEN 0.006
      WHEN 'SELLER' THEN 0.000
      WHEN 'VENDOR' THEN 0.000
      ELSE 0.000
    END
  )
)
WHERE status = 'ACTIVE';

COMMENT ON COLUMN organizations.config IS 'JSON: {feeRates: {marginRate}, settlement_cycle, features}';

-- =============================================================================
-- Verification Queries (for manual testing)
-- =============================================================================
-- SELECT merchant_name, config->'feeRates' as fee_rates FROM merchants WHERE status = 'ACTIVE';
-- SELECT org_name, org_type, config->'feeRates' as fee_rates FROM organizations WHERE status = 'ACTIVE';
-- =============================================================================
