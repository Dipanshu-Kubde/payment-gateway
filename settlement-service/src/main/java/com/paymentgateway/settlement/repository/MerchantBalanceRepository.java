package com.paymentgateway.settlement.repository;

import com.paymentgateway.settlement.entity.MerchantBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantBalanceRepository extends JpaRepository<MerchantBalance, Long> {

    Optional<MerchantBalance> findByMerchantId(Long merchantId);

    /**
     * Find all merchants with a positive pending (unsettled) balance,
     * used by the scheduled settlement batch job.
     */
    @Query("SELECT mb FROM MerchantBalance mb WHERE mb.pendingAmount > 0")
    List<MerchantBalance> findAllWithPendingBalance();
}
