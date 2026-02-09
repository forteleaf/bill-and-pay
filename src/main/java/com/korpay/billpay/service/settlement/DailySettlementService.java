package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.SettlementAccount;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.dto.response.*;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.OrganizationRepository;
import com.korpay.billpay.repository.SettlementAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private final OrganizationRepository organizationRepository;
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
                toOffsetDateTime(rs.getTimestamp("period_start")),
                toOffsetDateTime(rs.getTimestamp("period_end")),
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
            periodStart = toOffsetDateTime(row.get("period_start"));
            periodEnd = toOffsetDateTime(row.get("period_end"));
        }

        String breakdownSql = """
            SELECT
                s.merchant_id,
                m.name as merchant_name,
                m.merchant_code,
                m.settlement_cycle,
                o.org_code,
                COUNT(s.id) as transaction_count,
                COUNT(CASE WHEN s.entry_type = 'CREDIT' THEN 1 END) as approval_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END), 0) as approval_amount,
                COUNT(CASE WHEN s.entry_type = 'DEBIT' THEN 1 END) as cancel_count,
                COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN ABS(s.amount) ELSE 0 END), 0) as cancel_amount,
                COALESCE(AVG(s.fee_rate), 0) as fee_rate,
                COALESCE(SUM(s.fee_amount), 0) as fee_amount,
                COALESCE(SUM(s.net_amount), 0) as net_amount,
                CASE WHEN bool_and(s.status = 'COMPLETED') THEN 'COMPLETED'
                     WHEN bool_or(s.status = 'FAILED') THEN 'FAILED'
                     WHEN bool_or(s.status = 'ON_HOLD') THEN 'ON_HOLD'
                     ELSE 'PENDING' END as status,
                sa.bank_name,
                sa.account_number,
                sa.account_holder,
                string_agg(DISTINCT term.terminal_type::text, ',' ORDER BY term.terminal_type::text) as payment_type
            FROM settlements s
            JOIN settlement_batches sb ON s.settlement_batch_id = sb.id
            JOIN merchants m ON s.merchant_id = m.id
            LEFT JOIN organizations o ON m.org_id = o.id
            LEFT JOIN LATERAL (
                SELECT sa2.bank_name, sa2.account_number, sa2.account_holder
                FROM settlement_accounts sa2
                WHERE sa2.entity_type = 'MERCHANT'::contact_entity_type AND sa2.entity_id = s.merchant_id
                  AND sa2.deleted_at IS NULL
                ORDER BY sa2.is_primary DESC, sa2.created_at DESC
                LIMIT 1
            ) sa ON true
            LEFT JOIN transactions t ON s.transaction_id = t.id
            LEFT JOIN terminals term ON t.cat_id = term.cat_id
            WHERE s.entity_id = s.merchant_id
              AND sb.settlement_date = ?
            GROUP BY s.merchant_id, m.name, m.merchant_code, m.settlement_cycle,
                     o.org_code, sa.bank_name, sa.account_number, sa.account_holder
            ORDER BY approval_amount DESC
            """;

        List<MerchantSettlementBreakdownDto> merchantBreakdown = jdbcTemplate.query(breakdownSql, (rs, rowNum) ->
                new MerchantSettlementBreakdownDto(
                        rs.getObject("merchant_id", UUID.class),
                        rs.getString("merchant_name"),
                        rs.getString("merchant_code"),
                        rs.getLong("transaction_count"),
                        rs.getLong("approval_count"),
                        rs.getLong("approval_amount"),
                        rs.getLong("cancel_count"),
                        rs.getLong("cancel_amount"),
                        rs.getBigDecimal("fee_rate"),
                        rs.getLong("fee_amount"),
                        rs.getLong("net_amount"),
                        rs.getString("org_code"),
                        rs.getString("settlement_cycle"),
                        rs.getString("payment_type"),
                        rs.getString("bank_name"),
                        rs.getString("account_number"),
                        rs.getString("account_holder"),
                        rs.getString("status")
                ), date);

        String settlementsSql = """
            SELECT s.*, m.name as merchant_name FROM settlements s
            JOIN settlement_batches sb ON s.settlement_batch_id = sb.id
            LEFT JOIN merchants m ON s.merchant_id = m.id
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
                        .settledAt(toOffsetDateTime(rs.getTimestamp("settled_at")))
                        .createdAt(toOffsetDateTime(rs.getTimestamp("created_at")))
                        .updatedAt(toOffsetDateTime(rs.getTimestamp("updated_at")))
                        .merchantName(rs.getString("merchant_name"))
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

    public List<OrgDailySettlementSummaryDto> getOrgDailySettlementSummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                sb.settlement_date,
                MIN(sb.period_start) as period_start,
                MAX(sb.period_end) as period_end,
                COUNT(DISTINCT s.entity_id) as org_count,
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
            WHERE s.entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER')
              AND sb.settlement_date BETWEEN ? AND ?
            GROUP BY sb.settlement_date
            ORDER BY sb.settlement_date DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrgDailySettlementSummaryDto(
                rs.getObject("settlement_date", LocalDate.class),
                toOffsetDateTime(rs.getTimestamp("period_start")),
                toOffsetDateTime(rs.getTimestamp("period_end")),
                rs.getLong("org_count"),
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

    public OrgDailySettlementDetailDto getOrgDailySettlementDetail(LocalDate date) {
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
            periodStart = toOffsetDateTime(row.get("period_start"));
            periodEnd = toOffsetDateTime(row.get("period_end"));
        }

        String breakdownSql = """
            SELECT
                s.entity_id as org_id,
                o.name as org_name,
                s.entity_type as org_type,
                o.org_code as org_code,
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
            LEFT JOIN organizations o ON s.entity_id = o.id
            WHERE s.entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER')
              AND sb.settlement_date = ?
            GROUP BY s.entity_id, o.name, s.entity_type, o.org_code
            ORDER BY s.entity_type, net_amount DESC
            """;

        List<OrgSettlementBreakdownDto> orgBreakdown = jdbcTemplate.query(breakdownSql, (rs, rowNum) ->
                new OrgSettlementBreakdownDto(
                        rs.getObject("org_id", UUID.class),
                        rs.getString("org_name"),
                        rs.getString("org_type"),
                        rs.getString("org_code"),
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
            SELECT s.*, m.name as merchant_name FROM settlements s
            JOIN settlement_batches sb ON s.settlement_batch_id = sb.id
            LEFT JOIN merchants m ON s.merchant_id = m.id
            WHERE s.entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER')
              AND sb.settlement_date = ?
            ORDER BY s.entity_type, s.created_at DESC
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
                        .settledAt(toOffsetDateTime(rs.getTimestamp("settled_at")))
                        .createdAt(toOffsetDateTime(rs.getTimestamp("created_at")))
                        .updatedAt(toOffsetDateTime(rs.getTimestamp("updated_at")))
                        .merchantName(rs.getString("merchant_name"))
                        .build(),
                date);

        long totalTransactionCount = orgBreakdown.stream().mapToLong(OrgSettlementBreakdownDto::transactionCount).sum();
        long totalApprovalAmount = orgBreakdown.stream().mapToLong(OrgSettlementBreakdownDto::approvalAmount).sum();
        long totalCancelAmount = orgBreakdown.stream().mapToLong(OrgSettlementBreakdownDto::cancelAmount).sum();
        long totalFeeAmount = orgBreakdown.stream().mapToLong(OrgSettlementBreakdownDto::feeAmount).sum();
        long totalNetAmount = orgBreakdown.stream().mapToLong(OrgSettlementBreakdownDto::netAmount).sum();

        var summary = new OrgDailySettlementDetailDto.SummaryDto(
                totalTransactionCount,
                totalApprovalAmount,
                totalCancelAmount,
                totalFeeAmount,
                totalNetAmount
        );

        return new OrgDailySettlementDetailDto(
                date,
                batchNumber,
                periodStart,
                periodEnd,
                summary,
                orgBreakdown,
                settlements
        );
    }

    public OrgStatementDto getOrgStatement(UUID orgId, LocalDate startDate, LocalDate endDate) {
        Organization organization = organizationRepository.findByIdWithBusinessEntity(orgId)
                .orElseThrow(() -> new IllegalArgumentException("영업점을 찾을 수 없습니다: " + orgId));

        BusinessEntity businessEntity = organization.getBusinessEntity();
        String representativeName = businessEntity != null ? businessEntity.getRepresentativeName() : null;
        String businessNumber = businessEntity != null ? businessEntity.getBusinessNumber() : null;

        String bankName = null;
        String accountNumber = null;
        String accountHolder = null;

        var primaryAccount = settlementAccountRepository.findPrimaryByEntityTypeAndEntityId(
                ContactEntityType.BUSINESS_ENTITY, orgId);
        if (primaryAccount.isPresent()) {
            SettlementAccount account = primaryAccount.get();
            bankName = account.getBankName();
            accountNumber = account.getAccountNumber();
            accountHolder = account.getAccountHolder();
        } else {
            var accounts = settlementAccountRepository.findByEntityTypeAndEntityId(
                    ContactEntityType.BUSINESS_ENTITY, orgId);
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
            WHERE s.entity_id = ?
              AND sb.settlement_date BETWEEN ? AND ?
            GROUP BY sb.settlement_date, DATE(sb.period_start AT TIME ZONE 'Asia/Seoul')
            ORDER BY sb.settlement_date
            """;

        List<DailyOrgStatementRowDto> dailyDetails = jdbcTemplate.query(dailySql, (rs, rowNum) ->
                new DailyOrgStatementRowDto(
                        rs.getObject("settlement_date", LocalDate.class),
                        rs.getObject("transaction_date", LocalDate.class),
                        rs.getLong("transaction_count"),
                        rs.getLong("approval_count"),
                        rs.getLong("approval_amount"),
                        rs.getLong("cancel_count"),
                        rs.getLong("cancel_amount"),
                        rs.getLong("fee_amount"),
                        rs.getLong("net_amount")
                ), orgId, startDate, endDate);

        long totalApprovalAmount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::approvalAmount).sum();
        long totalApprovalCount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::approvalCount).sum();
        long totalCancelAmount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::cancelAmount).sum();
        long totalCancelCount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::cancelCount).sum();
        long totalFeeAmount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::feeAmount).sum();
        long totalNetAmount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::netAmount).sum();
        long totalTransactionCount = dailyDetails.stream().mapToLong(DailyOrgStatementRowDto::transactionCount).sum();
        long grossAmount = totalApprovalAmount - totalCancelAmount;

        BigDecimal feeRate = BigDecimal.ZERO;
        if (grossAmount > 0 && totalFeeAmount > 0) {
            feeRate = BigDecimal.valueOf(totalFeeAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(grossAmount), 4, RoundingMode.HALF_UP);
        }

        var summary = new OrgStatementDto.SummaryDto(
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

        return new OrgStatementDto(
                orgId,
                organization.getName(),
                organization.getOrgCode(),
                organization.getOrgType() != null ? organization.getOrgType().name() : null,
                businessNumber,
                representativeName,
                null,
                bankName,
                accountNumber,
                accountHolder,
                startDate.format(formatter),
                endDate.format(formatter),
                summary,
                dailyDetails
        );
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof Timestamp ts) return ts.toInstant().atZone(KST).toOffsetDateTime();
        return null;
    }
}
