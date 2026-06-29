package com.paymentgateway.payment.repository;

import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findByOrderId(String orderId);

    List<PaymentTransaction> findByMerchantId(Long merchantId);

    /**
     * Finds transactions that are scheduled for retry and whose retry time has arrived.
     * Used by the PaymentRetryService scheduled task.
     */
    List<PaymentTransaction> findByStatusAndNextRetryAtBefore(TransactionStatus status, LocalDateTime dateTime);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
