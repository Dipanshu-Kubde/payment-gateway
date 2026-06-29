package com.paymentgateway.settlement.service;

import com.paymentgateway.common.enums.SettlementStatus;
import com.paymentgateway.common.event.SettlementEvent;
import com.paymentgateway.common.exception.ResourceNotFoundException;
import com.paymentgateway.settlement.entity.MerchantBalance;
import com.paymentgateway.settlement.entity.Settlement;
import com.paymentgateway.settlement.kafka.SettlementEventPublisher;
import com.paymentgateway.settlement.repository.MerchantBalanceRepository;
import com.paymentgateway.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core settlement business logic.
 *
 * Runs a daily batch job (midnight) that iterates over all merchants with
 * pending balances, simulates a bank transfer, and records the result.
 * Also provides methods for manual settlement triggers, lookups, and balance queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    /** Default platform fee percentage (2%) */
    private static final BigDecimal DEFAULT_FEE_PERCENTAGE = new BigDecimal("0.02");

    /** Simulated bank transfer success rate (95%) */
    private static final double BANK_TRANSFER_SUCCESS_RATE = 0.95;

    private final SettlementRepository settlementRepository;
    private final MerchantBalanceRepository merchantBalanceRepository;
    private final SettlementEventPublisher settlementEventPublisher;
    private final SecureRandom random = new SecureRandom();

    // =========================================================================
    // Scheduled Settlement Batch
    // =========================================================================

    /**
     * Runs daily at midnight. Finds all merchants with pendingAmount > 0 and
     * processes a settlement for each.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void processSettlement() {
        log.info("=== Starting daily settlement batch at {} ===", LocalDateTime.now());

        List<MerchantBalance> pendingBalances = merchantBalanceRepository.findAllWithPendingBalance();

        if (pendingBalances.isEmpty()) {
            log.info("No merchants with pending balance. Settlement batch complete.");
            return;
        }

        log.info("Found {} merchant(s) with pending balance to settle", pendingBalances.size());

        int successCount = 0;
        int failureCount = 0;

        for (MerchantBalance balance : pendingBalances) {
            try {
                processSettlementForMerchant(balance);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Settlement failed for merchantId={}: {}",
                        balance.getMerchantId(), e.getMessage(), e);
            }
        }

        log.info("=== Settlement batch complete: {} succeeded, {} failed ===",
                successCount, failureCount);
    }

    // =========================================================================
    // Manual Settlement Trigger
    // =========================================================================

    /**
     * Admin endpoint to manually trigger settlement for a specific merchant.
     *
     * @param merchantId the merchant to settle
     * @return the created Settlement record
     */
    @Transactional
    public Settlement triggerManualSettlement(Long merchantId) {
        log.info("Manual settlement triggered for merchantId={}", merchantId);

        MerchantBalance balance = merchantBalanceRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MerchantBalance", "merchantId", merchantId));

        if (balance.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "No pending balance to settle for merchantId=" + merchantId);
        }

        return processSettlementForMerchant(balance);
    }

    // =========================================================================
    // Query Methods
    // =========================================================================

    /**
     * Lists all settlements for a given merchant.
     */
    @Transactional(readOnly = true)
    public List<Settlement> getSettlementsByMerchant(Long merchantId) {
        return settlementRepository.findByMerchantId(merchantId);
    }

    /**
     * Retrieves a single settlement by its public settlementId (UUID).
     */
    @Transactional(readOnly = true)
    public Settlement getSettlementById(String settlementId) {
        return settlementRepository.findBySettlementId(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Settlement", "settlementId", settlementId));
    }

    /**
     * Retrieves the current balance snapshot for a merchant.
     */
    @Transactional(readOnly = true)
    public MerchantBalance getMerchantBalance(Long merchantId) {
        return merchantBalanceRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MerchantBalance", "merchantId", merchantId));
    }

    // =========================================================================
    // Internal Settlement Processing
    // =========================================================================

    /**
     * Processes a settlement for a single merchant:
     * 1. Calculate gross, fee, and net amounts
     * 2. Create a PENDING settlement record
     * 3. Simulate bank transfer (2s delay, 95% success)
     * 4. Update status to COMPLETED or FAILED
     * 5. If COMPLETED, update the merchant balance
     * 6. Publish a SettlementEvent to Kafka
     */
    @Transactional
    protected Settlement processSettlementForMerchant(MerchantBalance balance) {
        Long merchantId = balance.getMerchantId();
        BigDecimal grossAmount = balance.getPendingAmount();

        // Step 1: Calculate amounts
        BigDecimal platformFee = grossAmount.multiply(DEFAULT_FEE_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(platformFee);

        log.info("Processing settlement for merchantId={}: gross={}, fee={}, net={}",
                merchantId, grossAmount, platformFee, netAmount);

        // Step 2: Create settlement record with PENDING status
        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .settlementDate(LocalDate.now())
                .grossAmount(grossAmount)
                .platformFee(platformFee)
                .netAmount(netAmount)
                .transactionCount(0) // Will be enriched when transaction tracking is added
                .status(SettlementStatus.PENDING)
                .build();

        settlement = settlementRepository.save(settlement);
        log.info("Created settlement record: settlementId={}", settlement.getSettlementId());

        // Step 3: Update to PROCESSING
        settlement.setStatus(SettlementStatus.PROCESSING);
        settlement = settlementRepository.save(settlement);

        // Step 4: Simulate bank transfer
        boolean transferSuccess = simulateBankTransfer();

        // Step 5: Update status based on result
        if (transferSuccess) {
            settlement.setStatus(SettlementStatus.COMPLETED);

            // Step 6: Update merchant balance
            balance.setSettledAmount(balance.getSettledAmount().add(netAmount));
            balance.setPendingAmount(BigDecimal.ZERO);
            balance.setLastSettlementDate(LocalDate.now());
            merchantBalanceRepository.save(balance);

            log.info("Settlement COMPLETED for merchantId={}: settlementId={}, netAmount={}",
                    merchantId, settlement.getSettlementId(), netAmount);
        } else {
            settlement.setStatus(SettlementStatus.FAILED);
            log.warn("Settlement FAILED for merchantId={}: settlementId={}",
                    merchantId, settlement.getSettlementId());
        }

        settlement = settlementRepository.save(settlement);

        // Step 7: Publish event to Kafka
        publishEvent(settlement);

        return settlement;
    }

    /**
     * Simulates a bank transfer with a 2-second delay and 95% success rate.
     */
    private boolean simulateBankTransfer() {
        try {
            log.info("Simulating bank transfer (2s delay)...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Bank transfer simulation interrupted");
            return false;
        }
        return random.nextDouble() < BANK_TRANSFER_SUCCESS_RATE;
    }

    /**
     * Builds and publishes a SettlementEvent to the settlement.completed Kafka topic.
     */
    private void publishEvent(Settlement settlement) {
        SettlementEvent event = SettlementEvent.builder()
                .settlementId(settlement.getSettlementId())
                .merchantId(settlement.getMerchantId())
                .grossAmount(settlement.getGrossAmount())
                .platformFee(settlement.getPlatformFee())
                .netAmount(settlement.getNetAmount())
                .transactionCount(settlement.getTransactionCount())
                .status(settlement.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        settlementEventPublisher.publishSettlementEvent(event);
    }
}
