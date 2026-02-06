package com.korpay.billpay.service.settlement;

import com.korpay.billpay.config.tenant.TenantContextHolder;
import com.korpay.billpay.config.tenant.TenantService;
import com.korpay.billpay.domain.enums.SettlementCycle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchScheduler {

    private final SettlementBatchService settlementBatchService;
    private final TenantService tenantService;

    @Value("${settlement.batch.enabled:false}")
    private boolean batchEnabled;

    @Scheduled(cron = "${settlement.batch.cron:0 0 1 * * *}")
    public void createDailyBatches() {
        if (!batchEnabled) {
            log.debug("Settlement batch scheduler disabled");
            return;
        }

        List<String> tenants = tenantService.getAllActiveTenants();
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

        log.info("Starting daily batch creation for {} tenants, target date: {}", tenants.size(), targetDate);

        for (String tenantId : tenants) {
            try {
                TenantContextHolder.runInTenant(tenantId, () -> {
                    log.info("Processing batches for tenant: {}", tenantId);

                    settlementBatchService.createDailyBatch(targetDate, SettlementCycle.D_PLUS_1);
                    settlementBatchService.createDailyBatch(targetDate, SettlementCycle.D_PLUS_3);
                });
            } catch (Exception e) {
                log.error("Failed to create batches for tenant {}: {}", tenantId, e.getMessage(), e);
            }
        }

        log.info("Daily batch creation completed");
    }
}
