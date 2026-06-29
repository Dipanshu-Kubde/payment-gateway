package com.paymentgateway.settlement.repository;

import com.paymentgateway.common.enums.SettlementStatus;
import com.paymentgateway.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findBySettlementId(String settlementId);

    List<Settlement> findByMerchantId(Long merchantId);

    List<Settlement> findByStatus(SettlementStatus status);

    List<Settlement> findByMerchantIdAndSettlementDateBetween(
            Long merchantId, LocalDate startDate, LocalDate endDate);
}
