package com.paymentgateway.payment.repository;

import com.paymentgateway.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    List<PaymentOrder> findByMerchantId(Long merchantId);

    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);

    List<PaymentOrder> findByMerchantIdAndStatus(Long merchantId, String status);
}
