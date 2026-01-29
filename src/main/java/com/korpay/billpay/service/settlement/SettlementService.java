package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.PaymentMethod;
import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.exception.settlement.SettlementCalculationException;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementCreationService settlementCreationService;
    private final MerchantRepository merchantRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public List<Settlement> processTransactionEvent(TransactionEvent event) {
        log.info("Processing settlement for transaction event {}: type={}, amount={}",
                event.getId(), event.getEventType(), event.getAmount());

        Merchant merchant = loadMerchant(event.getMerchantId());
        PaymentMethod paymentMethod = loadPaymentMethod(event.getPaymentMethodId());

        List<Settlement> settlements = settlementCreationService.createSettlements(
                event,
                merchant,
                paymentMethod.getMethodCode()
        );

        log.info("Successfully processed settlement for event {}: {} settlements created",
                event.getId(), settlements.size());

        return settlements;
    }

    @Transactional
    public List<Settlement> processTransactionEventWithMerchant(
            TransactionEvent event,
            Merchant merchant,
            String paymentMethodCode) {

        log.info("Processing settlement with provided merchant for event {}: type={}, amount={}",
                event.getId(), event.getEventType(), event.getAmount());

        List<Settlement> settlements = settlementCreationService.createSettlements(
                event,
                merchant,
                paymentMethodCode
        );

        log.info("Successfully processed settlement for event {}: {} settlements created",
                event.getId(), settlements.size());

        return settlements;
    }

    private Merchant loadMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new SettlementCalculationException(
                        "Merchant not found: " + merchantId
                ));
    }

    private PaymentMethod loadPaymentMethod(UUID paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new SettlementCalculationException(
                        "Payment method not found: " + paymentMethodId
                ));
    }
}
