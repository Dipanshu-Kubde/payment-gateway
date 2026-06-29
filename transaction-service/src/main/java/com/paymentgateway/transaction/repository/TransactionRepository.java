package com.paymentgateway.transaction.repository;

import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for transaction data access and analytics queries.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByMerchantId(Long merchantId, Pageable pageable);

    List<Transaction> findByStatus(TransactionStatus status);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByMerchantIdAndStatus(Long merchantId, TransactionStatus status, Pageable pageable);

    List<Transaction> findByMerchantIdAndCreatedAtBetween(Long merchantId, LocalDateTime start, LocalDateTime end);

    Page<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Transaction> findByMerchantIdAndCreatedAtBetween(Long merchantId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByStatus(TransactionStatus status);

    long countByMerchantId(Long merchantId);

    /**
     * Aggregate transaction statistics in a single query.
     * Returns: [totalCount, successCount, failedCount, pendingCount, totalSuccessAmount, fraudFlaggedCount]
     */
    @Query("SELECT COUNT(t), " +
            "SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.status IN ('INITIATED', 'PROCESSING', 'CREATED') THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN t.status = 'SUCCESS' THEN t.amount ELSE 0 END), 0), " +
            "SUM(CASE WHEN t.status = 'FRAUD_REVIEW' OR t.riskLevel = 'CRITICAL' OR t.riskLevel = 'HIGH' THEN 1 ELSE 0 END) " +
            "FROM Transaction t")
    Object[] getTransactionStats();

    /**
     * Aggregate transaction statistics for a specific merchant.
     */
    @Query("SELECT COUNT(t), " +
            "SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.status IN ('INITIATED', 'PROCESSING', 'CREATED') THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN t.status = 'SUCCESS' THEN t.amount ELSE 0 END), 0), " +
            "SUM(CASE WHEN t.status = 'FRAUD_REVIEW' OR t.riskLevel = 'CRITICAL' OR t.riskLevel = 'HIGH' THEN 1 ELSE 0 END) " +
            "FROM Transaction t WHERE t.merchantId = :merchantId")
    Object[] getTransactionStatsByMerchant(@Param("merchantId") Long merchantId);

    /**
     * Daily transaction count for the last N days.
     * Returns list of [date, count] pairs.
     */
    @Query("SELECT CAST(t.createdAt AS DATE), COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.createdAt >= :since " +
            "GROUP BY CAST(t.createdAt AS DATE) " +
            "ORDER BY CAST(t.createdAt AS DATE) ASC")
    List<Object[]> getDailyTransactionVolume(@Param("since") LocalDateTime since);

    /**
     * Payment method distribution (count per method).
     * Returns list of [paymentMethod, count] pairs.
     */
    @Query("SELECT t.paymentMethod, COUNT(t) " +
            "FROM Transaction t " +
            "GROUP BY t.paymentMethod")
    List<Object[]> getPaymentMethodDistribution();
}
