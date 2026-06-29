package com.paymentgateway.transaction.service;

import com.paymentgateway.common.dto.TransactionStatsDTO;
import com.paymentgateway.common.enums.PaymentMethod;
import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.exception.ResourceNotFoundException;
import com.paymentgateway.transaction.entity.Transaction;
import com.paymentgateway.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business logic for transaction queries and analytics.
 *
 * <p>All read methods are marked {@code @Transactional(readOnly = true)} to
 * hint the persistence provider to apply read-only optimizations.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    // ────────────────────────────────────────────────────────────────
    //  CRUD / Query Operations
    // ────────────────────────────────────────────────────────────────

    /**
     * Retrieve all transactions with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        log.debug("Fetching all transactions, page: {}", pageable);
        return transactionRepository.findAll(pageable);
    }

    /**
     * Retrieve a single transaction by its UUID-based transactionId.
     *
     * @throws ResourceNotFoundException if no matching record exists
     */
    @Transactional(readOnly = true)
    public Transaction getTransactionById(String transactionId) {
        log.debug("Fetching transaction: {}", transactionId);
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionId", transactionId));
    }

    /**
     * Retrieve transactions for a specific merchant with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByMerchant(Long merchantId, Pageable pageable) {
        log.debug("Fetching transactions for merchant: {}", merchantId);
        return transactionRepository.findByMerchantId(merchantId, pageable);
    }

    /**
     * Retrieve transactions filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByStatus(TransactionStatus status, Pageable pageable) {
        log.debug("Fetching transactions with status: {}", status);
        return transactionRepository.findByStatus(status, pageable);
    }

    /**
     * Retrieve transactions filtered by merchant and status.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByMerchantAndStatus(Long merchantId, TransactionStatus status, Pageable pageable) {
        log.debug("Fetching transactions for merchant: {} with status: {}", merchantId, status);
        return transactionRepository.findByMerchantIdAndStatus(merchantId, status, pageable);
    }

    /**
     * Retrieve transactions within a date range.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        log.debug("Fetching transactions between {} and {}", start, end);
        return transactionRepository.findByCreatedAtBetween(start, end, pageable);
    }

    /**
     * Retrieve transactions for a merchant within a date range.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByMerchantAndDateRange(Long merchantId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        log.debug("Fetching transactions for merchant: {} between {} and {}", merchantId, start, end);
        return transactionRepository.findByMerchantIdAndCreatedAtBetween(merchantId, start, end, pageable);
    }

    // ────────────────────────────────────────────────────────────────
    //  Analytics
    // ────────────────────────────────────────────────────────────────

    /**
     * Compute global transaction statistics: counts, rates, and revenue.
     */
    @Transactional(readOnly = true)
    public TransactionStatsDTO getTransactionStats() {
        log.debug("Computing global transaction statistics");
        Object[] stats = transactionRepository.getTransactionStats();
        return mapToStatsDTO(stats);
    }

    /**
     * Compute transaction statistics scoped to a single merchant.
     */
    @Transactional(readOnly = true)
    public TransactionStatsDTO getTransactionStatsByMerchant(Long merchantId) {
        log.debug("Computing transaction statistics for merchant: {}", merchantId);
        Object[] stats = transactionRepository.getTransactionStatsByMerchant(merchantId);
        return mapToStatsDTO(stats);
    }

    /**
     * Return daily transaction volumes for the last {@code days} days.
     * Gaps (dates with zero transactions) are filled so chart rendering is seamless.
     *
     * @param days number of past days to include (e.g. 7 for a week)
     * @return ordered list of maps with {@code date} and {@code count} keys
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyTransactionVolume(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        log.debug("Fetching daily transaction volume since: {}", since);

        List<Object[]> rawData = transactionRepository.getDailyTransactionVolume(since);

        // Build lookup from query results
        Map<LocalDate, Long> volumeMap = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else {
                date = (LocalDate) row[0];
            }
            Long count = ((Number) row[1]).longValue();
            volumeMap.put(date, count);
        }

        // Fill gaps with zeros
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate current = LocalDate.now().minusDays(days);
        LocalDate today = LocalDate.now();
        while (!current.isAfter(today)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", current.toString());
            entry.put("count", volumeMap.getOrDefault(current, 0L));
            result.add(entry);
            current = current.plusDays(1);
        }

        return result;
    }

    /**
     * Return transaction count per payment method for pie-chart rendering.
     *
     * @return map of {@link PaymentMethod} name to count
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getPaymentMethodDistribution() {
        log.debug("Fetching payment method distribution");
        List<Object[]> rawData = transactionRepository.getPaymentMethodDistribution();
        return rawData.stream()
                .collect(Collectors.toMap(
                        row -> ((PaymentMethod) row[0]).name(),
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    // ────────────────────────────────────────────────────────────────
    //  Event-Driven Mutations (called by Kafka consumer)
    // ────────────────────────────────────────────────────────────────

    /**
     * Create a new transaction record from a payment-initiated event.
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creating transaction record: {}", transaction.getTransactionId());
        return transactionRepository.save(transaction);
    }

    /**
     * Update an existing transaction (status, risk info, failure reason).
     *
     * @return the updated transaction, or {@code null} if not found (logged as warning)
     */
    @Transactional
    public Transaction updateTransaction(String transactionId, TransactionStatus status,
                                         Integer riskScore, RiskLevel riskLevel,
                                         String failureReason) {
        log.info("Updating transaction: {} → status={}", transactionId, status);
        Optional<Transaction> optionalTx = transactionRepository.findByTransactionId(transactionId);

        if (optionalTx.isEmpty()) {
            log.warn("Transaction not found for update: {}", transactionId);
            return null;
        }

        Transaction tx = optionalTx.get();
        tx.setStatus(status);

        if (riskScore != null) {
            tx.setRiskScore(riskScore);
        }
        if (riskLevel != null) {
            tx.setRiskLevel(riskLevel);
        }
        if (failureReason != null) {
            tx.setFailureReason(failureReason);
        }

        return transactionRepository.save(tx);
    }

    // ────────────────────────────────────────────────────────────────
    //  Private Helpers
    // ────────────────────────────────────────────────────────────────

    private TransactionStatsDTO mapToStatsDTO(Object[] stats) {
        long total = stats[0] != null ? ((Number) stats[0]).longValue() : 0;
        long success = stats[1] != null ? ((Number) stats[1]).longValue() : 0;
        long failed = stats[2] != null ? ((Number) stats[2]).longValue() : 0;
        long pending = stats[3] != null ? ((Number) stats[3]).longValue() : 0;
        BigDecimal revenue = stats[4] != null ? new BigDecimal(stats[4].toString()) : BigDecimal.ZERO;
        long fraudFlagged = stats[5] != null ? ((Number) stats[5]).longValue() : 0;

        double successRate = total > 0
                ? BigDecimal.valueOf(success).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double fraudRate = total > 0
                ? BigDecimal.valueOf(fraudFlagged).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return TransactionStatsDTO.builder()
                .totalTransactions(total)
                .successCount(success)
                .failedCount(failed)
                .pendingCount(pending)
                .fraudFlaggedCount(fraudFlagged)
                .totalRevenue(revenue)
                .successRate(successRate)
                .fraudRate(fraudRate)
                .build();
    }
}
