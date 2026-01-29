package com.korpay.billpay.service.webhook.verifier;

import com.korpay.billpay.exception.webhook.SignatureVerificationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String KORPAY_SIGNATURE_HEADER = "X-Korpay-Signature";

    public void verifyKorpaySignature(String rawBody, Map<String, String> headers, String secret) {
        String providedSignature = headers.get(KORPAY_SIGNATURE_HEADER);

        if (providedSignature == null || providedSignature.isEmpty()) {
            log.warn("Korpay signature header not found in request");
            throw new SignatureVerificationFailedException("Signature header missing");
        }

        String computedSignature = computeHmacSha256(rawBody, secret);

        if (!isEqual(providedSignature, computedSignature)) {
            log.warn("Korpay signature verification failed. Expected: {}, Provided: {}",
                    computedSignature, providedSignature);
            throw new SignatureVerificationFailedException("Signature verification failed");
        }

        log.debug("Korpay signature verified successfully");
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes());
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256", e);
            throw new SignatureVerificationFailedException("Signature computation failed", e);
        }
    }

    private boolean isEqual(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
