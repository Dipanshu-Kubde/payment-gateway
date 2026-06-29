package com.paymentgateway.fraud.repository;

import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.fraud.entity.FraudRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data-access layer for {@link FraudRecord} entities.
 *
 * <p>Spring Data JPA derives the query implementations automatically from the method
 * signatures — no boilerplate SQL required.
 */
@Repository
public interface FraudRecordRepository extends JpaRepository<FraudRecord, Long> {

    /** Find a fraud record by its unique transaction identifier. */
    Optional<FraudRecord> findByTransactionId(String transactionId);

    /** Paginated list of fraud records filtered by risk classification. */
    Page<FraudRecord> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    /** Paginated list of fraud records filtered by blocked status. */
    Page<FraudRecord> findByBlocked(Boolean blocked, Pageable pageable);

    /** Total number of transactions that have been blocked. */
    long countByBlocked(Boolean blocked);

    /** Count of transactions at a specific risk level. */
    long countByRiskLevel(RiskLevel riskLevel);

    /** Paginated list of all blocked/flagged transactions. */
    Page<FraudRecord> findByBlockedTrue(Pageable pageable);
}
