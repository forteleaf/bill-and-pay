package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.dto.settlement.FeeBreakdown;
import com.korpay.billpay.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCalculationService {

    private final FeeConfigResolver feeConfigResolver;
    private final OrganizationRepository organizationRepository;

    public List<Settlement> calculateFees(
            TransactionEvent event,
            Merchant merchant,
            String paymentMethodCode) {

        long eventAbsAmount = Math.abs(event.getAmount());
        boolean isCredit = event.getAmount() > 0;
        EntryType entryType = isCredit ? EntryType.CREDIT : EntryType.DEBIT;

        log.info("Calculating fees for event {}: amount={}, type={}, merchant={}",
                event.getId(), event.getAmount(), event.getEventType(), merchant.getId());

        List<Settlement> settlements = new ArrayList<>();
        List<FeeBreakdown> breakdowns = new ArrayList<>();

        BigDecimal merchantFeeRate = feeConfigResolver.resolveMerchantFeeRate(merchant, paymentMethodCode);
        long merchantFeeAmount = calculateFeeAmount(eventAbsAmount, merchantFeeRate);
        long merchantSettlementAmount = eventAbsAmount - merchantFeeAmount;

        long signedMerchantSettlement = isCredit ? merchantSettlementAmount : -merchantSettlementAmount;
        long signedMerchantFee = isCredit ? merchantFeeAmount : -merchantFeeAmount;

        Settlement merchantSettlement = buildMerchantSettlement(
                event, merchant, entryType, signedMerchantSettlement, signedMerchantFee, merchantFeeRate);
        settlements.add(merchantSettlement);

        breakdowns.add(FeeBreakdown.builder()
                .entityId(merchant.getId())
                .entityType(merchant.getOrganization().getOrgType())
                .feeRate(merchantFeeRate)
                .settlementAmount(signedMerchantSettlement)
                .description("Merchant settlement")
                .build());

        List<Organization> ancestors = organizationRepository.findAncestors(merchant.getOrgPath());
        ancestors.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));

        BigDecimal previousFeeRate = merchantFeeRate;
        for (Organization org : ancestors) {
            BigDecimal orgFeeRate = feeConfigResolver.resolveOrganizationFeeRate(org, paymentMethodCode);

            // DISTRIBUTOR는 마스터 잔여금(residual)으로 일괄 처리
            if (org.getOrgType() == OrganizationType.DISTRIBUTOR) {
                previousFeeRate = orgFeeRate;
                continue;
            }

            BigDecimal marginRate = previousFeeRate.subtract(orgFeeRate);

            if (marginRate.compareTo(BigDecimal.ZERO) > 0) {
                long marginAmount = calculateFeeAmount(eventAbsAmount, marginRate);
                long signedMargin = isCredit ? marginAmount : -marginAmount;

                Settlement orgSettlement = buildOrganizationSettlement(
                        event, org, entryType, signedMargin, marginRate);
                settlements.add(orgSettlement);

                breakdowns.add(FeeBreakdown.builder()
                        .entityId(org.getId())
                        .entityType(org.getOrgType())
                        .entityPath(org.getPath())
                        .feeRate(orgFeeRate)
                        .marginRate(marginRate)
                        .marginAmount(signedMargin)
                        .settlementAmount(signedMargin)
                        .description(org.getOrgType() + " margin")
                        .build());
            }

            previousFeeRate = orgFeeRate;
        }

        long totalAllocatedAbs = settlements.stream()
                .mapToLong(s -> Math.abs(s.getAmount()))
                .sum();
        long masterResidualAbs = eventAbsAmount - totalAllocatedAbs;

        if (masterResidualAbs > 0) {
            long signedResidual = isCredit ? masterResidualAbs : -masterResidualAbs;

            Organization distributor = ancestors.stream()
                    .filter(org -> org.getOrgType() == OrganizationType.DISTRIBUTOR)
                    .findFirst()
                    .orElse(null);

            Settlement masterSettlement = buildMasterSettlement(
                    event, distributor, entryType, signedResidual, previousFeeRate);
            settlements.add(masterSettlement);

            breakdowns.add(FeeBreakdown.builder()
                    .entityId(distributor != null ? distributor.getId() : null)
                    .entityType(OrganizationType.DISTRIBUTOR)
                    .entityPath(distributor != null ? distributor.getPath() : null)
                    .marginRate(previousFeeRate)
                    .marginAmount(signedResidual)
                    .settlementAmount(signedResidual)
                    .description("Master residual")
                    .build());
        }

        logFeeBreakdown(event.getId(), breakdowns, isCredit ? eventAbsAmount : -eventAbsAmount);

        return settlements;
    }

    private long calculateFeeAmount(long amount, BigDecimal feeRate) {
        return BigDecimal.valueOf(amount)
                .multiply(feeRate)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
    }

    private Settlement buildMerchantSettlement(
            TransactionEvent event,
            Merchant merchant,
            EntryType entryType,
            long settlementAmount,
            long feeAmount,
            BigDecimal feeRate) {

        return Settlement.builder()
                .transactionEventId(event.getId())
                .transactionId(event.getTransactionId())
                .merchantId(merchant.getId())
                .orgPath(merchant.getOrgPath())
                .entityId(merchant.getId())
                .entityType(merchant.getOrganization().getOrgType())
                .entityPath(merchant.getOrgPath())
                .entryType(entryType)
                .amount(settlementAmount)
                .feeAmount(0L)
                .netAmount(settlementAmount)
                .currency(event.getCurrency())
                .feeRate(feeRate)
                .status(SettlementStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private Settlement buildOrganizationSettlement(
            TransactionEvent event,
            Organization org,
            EntryType entryType,
            long marginAmount,
            BigDecimal marginRate) {

        return Settlement.builder()
                .transactionEventId(event.getId())
                .transactionId(event.getTransactionId())
                .merchantId(event.getMerchantId())
                .orgPath(event.getOrgPath())
                .entityId(org.getId())
                .entityType(org.getOrgType())
                .entityPath(org.getPath())
                .entryType(entryType)
                .amount(marginAmount)
                .feeAmount(0L)
                .netAmount(marginAmount)
                .currency(event.getCurrency())
                .feeRate(marginRate)
                .status(SettlementStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private Settlement buildMasterSettlement(
            TransactionEvent event,
            Organization distributor,
            EntryType entryType,
            long residualAmount,
            BigDecimal feeRate) {

        return Settlement.builder()
                .transactionEventId(event.getId())
                .transactionId(event.getTransactionId())
                .merchantId(event.getMerchantId())
                .orgPath(event.getOrgPath())
                .entityId(distributor != null ? distributor.getId() : null)
                .entityType(OrganizationType.DISTRIBUTOR)
                .entityPath(distributor != null ? distributor.getPath() : "master")
                .entryType(entryType)
                .amount(residualAmount)
                .feeAmount(0L)
                .netAmount(residualAmount)
                .currency(event.getCurrency())
                .feeRate(feeRate)
                .status(SettlementStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private void logFeeBreakdown(Object eventId, List<FeeBreakdown> breakdowns, long total) {
        log.info("Fee breakdown for event {}:", eventId);
        breakdowns.forEach(b ->
                log.info("  {} {}: rate={}, margin={}, amount={}",
                        b.getEntityType() != null ? b.getEntityType() : "MERCHANT",
                        b.getEntityId() != null ? b.getEntityId() : "MASTER",
                        b.getFeeRate(),
                        b.getMarginRate(),
                        b.getSettlementAmount())
        );
        log.info("  Total: {}", total);
    }
}
