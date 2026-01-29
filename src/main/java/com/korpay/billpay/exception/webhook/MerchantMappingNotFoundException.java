package com.korpay.billpay.exception.webhook;

/**
 * Exception thrown when merchant mapping is not found for a PG merchant number
 * This is a business-level error that should be saved to unmapped_transactions
 */
public class MerchantMappingNotFoundException extends WebhookException {

    private final String pgCode;
    private final String pgMerchantNo;

    public MerchantMappingNotFoundException(String pgCode, String pgMerchantNo) {
        super(String.format("Merchant mapping not found for PG: %s, Merchant No: %s", pgCode, pgMerchantNo));
        this.pgCode = pgCode;
        this.pgMerchantNo = pgMerchantNo;
    }

    public String getPgCode() {
        return pgCode;
    }

    public String getPgMerchantNo() {
        return pgMerchantNo;
    }
}
