package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.SettlementAccount;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.dto.response.*;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.SettlementAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailySettlementService {

    private final JdbcTemplate jdbcTemplate;
    private final MerchantRepository merchantRepository;
    private final SettlementAccountRepository settlementAccountRepository;

    public List<DailySettlementSummaryDto> getDailySettlementSummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                sb.settlement_date,
                MIN(sb.period_start) as period_start,
                MAX(sb.period_end) as period_end,
                COUNT(DISTINCT s.merchant_id) as merchant_count,
                COUNT(s.id) as transaction_count,
                COUNT(CASE WHEN s.entry_type = 'CREDIT' THEN 1 END) as approval_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END), 0) as approval_amount,
                COUNT(CASE WHEN s.entry_type = 'DEBIT' THEN 1 END) as cancel_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN ABS(s.amount) ELSE 0 END), 0) as cancel_amount,
                COALESCE(SUM(s.fee_amount), 0) as fee_amount,
                COALESCE(SUM(s.net_amount), 0) as net_amount,
                CASE WHEN bool_and(sb.status = 'COMPLETED') THEN 'COMPLETED' ELSE 'SCHEDULED' END as status
            FROM settlement_batches sb
            JOIN settlements s ON s.settlement_batch_id = sb.id
            WHERE s.entity_id = s.merchant_id
              AND sb.settlement_date BETWEEN ? AND ?
            GROUP BY sb.settlement_date
            ORDER BY sb.settlement_date DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DailySettlementSummaryDto(
                rs.getObject("settlement_date", LocalDate.class),
                rs.getObject("period_start", OffsetDateTime.class),
                rs.getObject("period_end", OffsetDateTime.class),
                rs.getLong("merchant_count"),
                rs.getLong("transaction_count"),
                rs.getLong("approval_count"),
                rs.getLong("approval_amount"),
                rs.getLong("cancel_count"),
                rs.getLong("cancel_amount"),
                rs.getLong("fee_amount"),
                rs.getLong("net_amount"),
                rs.getString("status")
        ), startDate, endDate);
    }

    public DailySettlementDetailDto getDailySettlementDetail(LocalDate date) {
        String batchSql = """
            SELECT
                sb.batch_number,
                MIN(sb.period_start) as period_start,
                MAX(sb.period_end) as period_end
            FROM settlement_batches sb
            WHERE sb.settlement_date = ?
            GROUP BY sb.batch_number
            ORDER BY sb.batch_number
            LIMIT 1
            """;

        String batchNumber = null;
        OffsetDateTime periodStart = null;
        OffsetDateTime periodEnd = null;

        var batchRows = jdbcTemplate.queryForList(batchSql, date);
        if (!batchRows.isEmpty()) {
            var row = batchRows.getFirst();
            batchNumber = (String) row.get("batch_number");
            periodStart = (OffsetDateTime) row.get("period_start");
            periodEnd = (OffsetDateTime) row.get("period_end");
        }

        String breakdownSql = """
            SELECT
                s.merchant_id,
                m.name as merchant_name,
                COUNT(s.id) as transaction_count,
                COUNT(CASE WHEN s.entry_type = 'CREDIT' THEN 1 END) as approval_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END), 0) as approval_amount,
                COUNT(CASE WHEN s.entry_type = 'DEBIT' THEN 1 END) as cancel_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN ABS(s.amount) ELSE 0 END), 0) as cancel_amount,
                COALESCE(AVG(s.fee_rate), 0) as fee_rate,
                COALESCE(SUM(s.fee_amount), 0) as fee_amount,
                COALESCE(SUM(s.net_amount), 0) as net_amount
            FROM settlements s
            JOIN settlement_batches sb ON s.settlement_batch_id = sb.id
            JOIN merchants m ON s.merchant_id = m.id
            WHERE s.entity_id = s.merchant_id
              AND sb.settlement_date = ?
            GROUP BY s.merchant_id, m.name
            ORDER BY approval_amount DESC
            """;

        List<MerchantSettlementBreakdownDto> merchantBreakdown = jdbcTemplate.query(breakdownSql, (rs, rowNum) ->
                new MerchantSettlementBreakdownDto(
                        rs.getObject("merchant_id", UUID.class),
                        rs.getString("merchant_name"),
                        rs.getLong("transaction_count"),
                        rs.getLong("approval_count"),
                        rs.getLong("approval_amount"),
                        rs.getLong("cancel_count"),
                        rs.getLong("cancel_amount"),
                        rs.getBigDecimal("fee_rate"),
                        rs.getLong("fee_amount"),
                        rs.getLong("net_amount")
                ), date);

        String settlementsSql = """
            SELECT s.* FROM settlements s
            JOIN settlement_batches sb ON s.settlement_batch_id = sb.id
            WHERE s.entity_id = s.merchant_id
              AND sb.settlement_date = ?
            ORDER BY s.created_at DESC
            """;

        List<SettlementDto> settlements = jdbcTemplate.query(settlementsSql, (rs, rowNum) ->
                SettlementDto.builder()
                        .id(rs.getObject("id", UUID.class))
                        .settlementBatchId(rs.getObject("settlement_batch_id", UUID.class))
                        .transactionEventId(rs.getObject("transaction_event_id", UUID.class))
                        .transactionId(rs.getObject("transaction_id", UUID.class))
                        .merchantId(rs.getObject("merchant_id", UUID.class))
                        .orgPath(rs.getString("org_path"))
                        .entityId(rs.getObject("entity_id", UUID.class))
                        .entityType(rs.getString("entity_type") != null
                                ? com.korpay.billpay.domain.enums.OrganizationType.valueOf(rs.getString("entity_type"))
                                : null)
                        .entityPath(rs.getString("entity_path"))
                        .entryType(rs.getString("entry_type") != null
                                ? com.korpay.billpay.domain.enums.EntryType.valueOf(rs.getString("entry_type"))
                                : null)
                        .amount(rs.getLong("amount"))
                        .feeAmount(rs.getLong("fee_amount"))
                        .netAmount(rs.getLong("net_amount"))
                        .currency(rs.getString("currency"))
                        .feeRate(rs.getBigDecimal("fee_rate"))
                        .status(rs.getString("status") != null
                                ? com.korpay.billpay.domain.enums.SettlementStatus.valueOf(rs.getString("status"))
                                : null)
                        .settledAt(rs.getObject("settled_at", OffsetDateTime.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                        .build(),
                date);

        long totalTransactionCount = merchantBreakdown.stream().mapToLong(MerchantSettlementBreakdownDto::transactionCount).sum();
        long totalApprovalAmount = merchantBreakdown.stream().mapToLong(MerchantSettlementBreakdownDto::approvalAmount).sum();
        long totalCancelAmount = merchantBreakdown.stream().mapToLong(MerchantSettlementBreakdownDto::cancelAmount).sum();
        long totalFeeAmount = merchantBreakdown.stream().mapToLong(MerchantSettlementBreakdownDto::feeAmount).sum();
        long totalNetAmount = merchantBreakdown.stream().mapToLong(MerchantSettlementBreakdownDto::netAmount).sum();

        var summary = new DailySettlementDetailDto.SummaryDto(
                totalTransactionCount,
                totalApprovalAmount,
                totalCancelAmount,
                totalFeeAmount,
                totalNetAmount
        );

        return new DailySettlementDetailDto(
                date,
                batchNumber,
                periodStart,
                periodEnd,
                summary,
                merchantBreakdown,
                settlements
        );
    }

    public MerchantStatementDto getMerchantStatement(UUID merchantId, LocalDate startDate, LocalDate endDate) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("가맹점을 찾을 수 없습니다: " + merchantId));

        Organization organization = merchant.getOrganization();
        BusinessEntity businessEntity = organization != null ? organization.getBusinessEntity() : null;

        String representativeName = businessEntity != null ? businessEntity.getRepresentativeName() : null;
        String businessNumber = merchant.getBusinessNumber();
        String settlementCycle = merchant.getSettlementCycle() != null ? merchant.getSettlementCycle().name() : null;

        String bankName = null;
        String accountNumber = null;
        String accountHolder = null;

        var primaryAccount = settlementAccountRepository.findPrimaryByEntityTypeAndEntityId(
                ContactEntityType.MERCHANT, merchantId);
        if (primaryAccount.isPresent()) {
            SettlementAccount account = primaryAccount.get();
            bankName = account.getBankName();
            accountNumber = account.getAccountNumber();
            accountHolder = account.getAccountHolder();
        } else {
            var accounts = settlementAccountRepository.findByEntityTypeAndEntityId(
                    ContactEntityType.MERCHANT, merchantId);
            if (!accounts.isEmpty()) {
                SettlementAccount account = accounts.getFirst();
                bankName = account.getBankName();
                accountNumber = account.getAccountNumber();
                accountHolder = account.getAccountHolder();
            }
        }

        String dailySql = """
            SELECT
                sb.settlement_date,
                DATE(sb.period_start AT TIME ZONE 'Asia/Seoul') as transaction_date,
                COUNT(s.id) as transaction_count,
                COUNT(CASE WHEN s.entry_type = 'CREDIT' THEN 1 END) as approval_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END), 0) as approval_amount,
                COUNT(CASE WHEN s.entry_type = 'DEBIT' THEN 1 END) as cancel_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN ABS(s.amount) ELSE 0 END), 0) as cancel_amount,
                COALESCE(SUM(s.fee_amount), 0) as fee_amount,
                COALESCE(SUM(s.net_amount), 0) as net_amount
            FROM settlements s
            JOIN settlement_batches sb ON s.settlement_batch_id = sb.id
            WHERE s.merchant_id = ?
              AND s.entity_id = s.merchant_id
              AND sb.settlement_date BETWEEN ? AND ?
            GROUP BY sb.settlement_date, DATE(sb.period_start AT TIME ZONE 'Asia/Seoul')
            ORDER BY sb.settlement_date
            """;

        List<DailyStatementRowDto> dailyDetails = jdbcTemplate.query(dailySql, (rs, rowNum) ->
                new DailyStatementRowDto(
                        rs.getObject("settlement_date", LocalDate.class),
                        rs.getObject("transaction_date", LocalDate.class),
                        rs.getLong("transaction_count"),
                        rs.getLong("approval_count"),
                        rs.getLong("approval_amount"),
                        rs.getLong("cancel_count"),
                        rs.getLong("cancel_amount"),
                        rs.getLong("fee_amount"),
                        rs.getLong("net_amount")
                ), merchantId, startDate, endDate);

        long totalApprovalAmount = dailyDetails.stream().mapToLong(DailyStatementRowDto::approvalAmount).sum();
        long totalApprovalCount = dailyDetails.stream().mapToLong(DailyStatementRowDto::approvalCount).sum();
        long totalCancelAmount = dailyDetails.stream().mapToLong(DailyStatementRowDto::cancelAmount).sum();
        long totalCancelCount = dailyDetails.stream().mapToLong(DailyStatementRowDto::cancelCount).sum();
        long totalFeeAmount = dailyDetails.stream().mapToLong(DailyStatementRowDto::feeAmount).sum();
        long totalNetAmount = dailyDetails.stream().mapToLong(DailyStatementRowDto::netAmount).sum();
        long totalTransactionCount = dailyDetails.stream().mapToLong(DailyStatementRowDto::transactionCount).sum();
        long grossAmount = totalApprovalAmount - totalCancelAmount;

        BigDecimal feeRate = BigDecimal.ZERO;
        if (grossAmount > 0 && totalFeeAmount > 0) {
            feeRate = BigDecimal.valueOf(totalFeeAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(grossAmount), 4, RoundingMode.HALF_UP);
        }

        var summary = new MerchantStatementDto.SummaryDto(
                totalApprovalAmount,
                totalApprovalCount,
                totalCancelAmount,
                totalCancelCount,
                grossAmount,
                feeRate,
                totalFeeAmount,
                totalNetAmount,
                totalTransactionCount
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return new MerchantStatementDto(
                merchantId,
                merchant.getName(),
                merchant.getMerchantCode(),
                businessNumber,
                representativeName,
                settlementCycle,
                bankName,
                accountNumber,
                accountHolder,
                startDate.format(formatter),
                endDate.format(formatter),
                summary,
                dailyDetails
        );
    }
}
