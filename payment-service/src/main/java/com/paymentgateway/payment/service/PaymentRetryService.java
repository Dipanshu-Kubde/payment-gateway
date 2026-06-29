package com.paymentgateway.payment.service;

import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.kafka.PaymentEventPublisher;
import com.paymentgateway.payment.repository.PaymentOrderRepository;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that automatically retries failed payment transactions.
 * Runs every 30 seconds, finds transactions with RETRY_SCHEDULED status whose
 * nextRetryAt time has passed, and retries them with exponential backoff.
 *
 * Backoff schedule (base delays):
 *   Retry 1: 1 second
 *   Retry 2: 2 seconds
 *   Retry 3: 4 seconds
 *
 * After max retries (default 3), the transaction is marked as permanently FAILED.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentRetryService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentOrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Scheduled task that processes pending retries every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processRetries() {
        LocalDateTime now = LocalDateTime.now();

        List<PaymentTransaction> retryableTransactions =
                transactionRepository.findByStatusAndNextRetryAtBefore(
                        TransactionStatus.RETRY_SCHEDULED, now);

        if (retryableTransactions.isEmpty()) {
            return;
        }

        log.info("Found {} transactions eligible for retry", retryableTransactions.size());

        for (PaymentTransaction transaction : retryableTransactions) {
            retryTransaction(transaction);
        }
    }

    /**
     * Retries a single transaction with exponential backoff logic.
     */
    private void retryTransaction(PaymentTransaction transaction) {
        log.info("Retrying transaction: {}, attempt: {}/{}",
                transaction.getTransactionId(),
                transaction.getRetryCount() + 1,
                transaction.getMaxRetries());

        try {
            // Increment retry count
            transaction.setRetryCount(transaction.getRetryCount() + 1);
            transaction.setFailureReason(null);
            transaction.setNextRetryAt(null);

            // Simulate the payment again
            paymentService.simulatePayment(transaction);

            // Handle result
            if (transaction.getStatus() == TransactionStatus.SUCCESS) {
                log.info("Retry SUCCEEDED for transaction: {} on attempt {}",
                        transaction.getTransactionId(), transaction.getRetryCount());

                // Update the parent order status
                orderRepository.findByOrderId(transaction.getOrderId())
                        .ifPresent(order -> {
                            order.setStatus("PAID");
                            orderRepository.save(order);
                        });

            } else if (transaction.getStatus() == TransactionStatus.RETRY_SCHEDULED) {
                // Still timing out, check if retries remain
                if (transaction.canRetry()) {
                    long delaySeconds = transaction.getNextRetryDelaySeconds();
                    transaction.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
                    log.warn("Transaction {} still failing. Next retry in {}s at {}",
                            transaction.getTransactionId(), delaySeconds, transaction.getNextRetryAt());
                } else {
                    // Retries exhausted
                    markAsFailed(transaction);
                }

            } else if (transaction.getStatus() == TransactionStatus.FAILED) {
                // Payment failed, check if retries remain
                if (transaction.canRetry()) {
                    long delaySeconds = transaction.getNextRetryDelaySeconds();
                    transaction.setStatus(TransactionStatus.RETRY_SCHEDULED);
                    transaction.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
                    log.warn("Transaction {} failed. Scheduling retry in {}s",
                            transaction.getTransactionId(), delaySeconds);
                } else {
                    markAsFailed(transaction);
                }
            }

            transactionRepository.save(transaction);

            // Publish processed event for downstream services
            publishProcessedEvent(transaction);

        } catch (Exception e) {
            log.error("Error retrying transaction: {}", transaction.getTransactionId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Retry error: " + e.getMessage());
            transactionRepository.save(transaction);
        }
    }

    /**
     * Marks a transaction as permanently failed after exhausting all retries.
     */
    private void markAsFailed(PaymentTransaction transaction) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setNextRetryAt(null);
        String reason = transaction.getFailureReason() != null
                ? transaction.getFailureReason()
                : "Unknown error";
        transaction.setFailureReason("Max retries exhausted (" + transaction.getMaxRetries() + "). Last: " + reason);

        log.error("Transaction {} permanently FAILED after {} retries",
                transaction.getTransactionId(), transaction.getRetryCount());
    }

    private void publishProcessedEvent(PaymentTransaction transaction) {
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .transactionId(transaction.getTransactionId())
                .orderId(transaction.getOrderId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .riskScore(transaction.getRiskScore())
                .customerEmail(transaction.getCustomerEmail())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPaymentProcessed(event);
    }
}
