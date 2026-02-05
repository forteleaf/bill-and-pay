package com.korpay.billpay.service.webhook.adapter;

import com.korpay.billpay.domain.enums.EventType;
import com.korpay.billpay.dto.webhook.KorpayWebhookData;
import com.korpay.billpay.dto.webhook.TransactionDto;
import com.korpay.billpay.exception.webhook.WebhookProcessingException;
import com.korpay.billpay.service.webhook.verifier.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KorpayWebhookAdapter implements PgWebhookAdapter {

    private static final String PG_CODE = "KORPAY";
    private static final DateTimeFormatter KORPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_CURRENCY = "KRW";

    private final WebhookSignatureVerifier signatureVerifier;

    @Override
    public TransactionDto parse(String rawBody, Map<String, String> headers) {
        log.debug("Parsing KORPAY webhook data");

        KorpayWebhookData data = parseFormData(rawBody);

        EventType eventType = determineEventType(data);
        boolean isCancel = "Y".equals(data.getCancelYN());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("connCd", data.getConnCd());
        metadata.put("ediNo", data.getEdiNo());
        metadata.put("notiDnt", data.getNotiDnt());
        if (data.getUsePointAmt() != null) {
            metadata.put("usePointAmt", data.getUsePointAmt());
        }
        if (data.getResultCd() != null) {
            metadata.put("resultCd", data.getResultCd());
        }

        return TransactionDto.builder()
                .pgTid(data.getTid())
                .pgOtid(data.getOtid())
                .pgMerchantNo(data.getMid())
                .terminalId(data.getCatId())
                .channelType(data.getConnCd())
                .vanTid(data.getEdiNo())
                .orderId(data.getOrdNo())
                .amount(data.getAmt())
                .remainAmount(data.getRemainAmt() != null ? data.getRemainAmt() : 0L)
                .paymentMethod(data.getPayMethod())
                .goodsName(data.getGoodsName())
                .currency(DEFAULT_CURRENCY)
                .cardNoMasked(data.getCardNo())
                .approvalNo(data.getAppNo())
                .installment(parseInstallment(data.getQuota()))
                .issuerCode(data.getAppCardCd())
                .acquirerCode(data.getAcqCardCd())
                .cardCompanyName(data.getFnNm())
                .buyerName(data.getOrdNm())
                .buyerId(data.getBuyerId())
                .eventType(eventType)
                .isCancel(isCancel)
                .transactedAt(parseDateTime(data.getAppDtm()))
                .canceledAt(isCancel ? parseDateTime(data.getCcDnt()) : null)
                .metadata(metadata)
                .build();
    }

    @Override
    public boolean verifySignature(String rawBody, Map<String, String> headers, String secret) {
        // Parse connCd from rawBody to determine if it's a terminal transaction
        String connCd = extractConnCd(rawBody);
        
        // Terminal transactions (connCd=0003) don't have signature - skip verification
        if ("0003".equals(connCd)) {
            log.debug("Skipping signature verification for terminal transaction (connCd=0003)");
            return true;
        }
        
        // Online transactions (connCd=0005) require signature verification
        try {
            signatureVerifier.verifyKorpaySignature(rawBody, headers, secret);
            return true;
        } catch (Exception e) {
            log.warn("KORPAY signature verification failed for online transaction", e);
            return false;
        }
    }
    
    private String extractConnCd(String rawBody) {
        try {
            String[] pairs = rawBody.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2 && "connCd".equals(keyValue[0])) {
                    return java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract connCd from body", e);
        }
        return null;
    }

    @Override
    public String getPgCode() {
        return PG_CODE;
    }

    private EventType determineEventType(KorpayWebhookData data) {
        if (!"Y".equals(data.getCancelYN())) {
            return EventType.APPROVAL;
        }

        if (data.getRemainAmt() != null && data.getRemainAmt() > 0) {
            return EventType.PARTIAL_CANCEL;
        }

        return EventType.CANCEL;
    }

    private KorpayWebhookData parseFormData(String rawBody) {
        Map<String, String> params = new HashMap<>();

        try {
            String[] pairs = rawBody.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new WebhookProcessingException("Failed to parse form data", e);
        }

        return KorpayWebhookData.builder()
                .tid(params.get("tid"))
                .otid(params.get("otid"))
                .mid(params.get("mid"))
                .catId(params.get("catId"))
                .connCd(params.get("connCd"))
                .ediNo(params.get("ediNo"))
                .ordNo(params.get("ordNo"))
                .amt(parseLong(params.get("amt")))
                .remainAmt(parseLong(params.get("remainAmt")))
                .payMethod(params.get("payMethod"))
                .goodsName(params.get("goodsName"))
                .cardNo(params.get("cardNo"))
                .appNo(params.get("appNo"))
                .quota(params.get("quota"))
                .appCardCd(params.get("appCardCd"))
                .acqCardCd(params.get("acqCardCd"))
                .fnNm(params.get("fnNm"))
                .ordNm(params.get("ordNm"))
                .buyerId(params.get("buyerId"))
                .cancelYN(params.get("cancelYN"))
                .appDtm(params.get("appDtm"))
                .ccDnt(params.get("ccDnt"))
                .notiDnt(params.get("notiDnt"))
                .tPhone(params.get("tPhone"))
                .usePointAmt(params.get("usePointAmt"))
                .vacntNo(params.get("vacntNo"))
                .socHpNo(params.get("socHpNo"))
                .charSet(params.get("charSet"))
                .hashStr(params.get("hashStr"))
                .ediDate(params.get("ediDate"))
                .resultCd(params.get("resultCd"))
                .cashCrctFlg(params.get("cashCrctFlg"))
                .build();
    }

    private OffsetDateTime parseDateTime(String dtm) {
        if (dtm == null || dtm.isEmpty()) {
            return null;
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dtm, KORPAY_DATE_FORMAT);
            return localDateTime.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse KORPAY datetime: {}", dtm, e);
            return null;
        }
    }

    private Integer parseInstallment(String quota) {
        if (quota == null || quota.isEmpty() || "00".equals(quota)) {
            return 0;
        }

        try {
            return Integer.parseInt(quota);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse installment: {}", quota);
            return 0;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value: {}", value);
            return null;
        }
    }
}
