package com.paymentgateway.payment.kafka;

import com.paymentgateway.common.event.PaymentInitiatedEvent;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes payment lifecycle events to Kafka topics.
 * - payment.initiated: When a payment processing starts (consumed by Fraud & Transaction services)
 * - payment.processed: When a payment completes/fails (consumed by Transaction, Notification & Settlement services)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final String TOPIC_PAYMENT_INITIATED = "payment.initiated";
    private static final String TOPIC_PAYMENT_PROCESSED = "payment.processed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a PaymentInitiatedEvent when payment processing begins.
     * Uses the transactionId as the Kafka message key for partition affinity.
     */
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Publishing PaymentInitiatedEvent for transaction: {}, order: {}",
                event.getTransactionId(), event.getOrderId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_PAYMENT_INITIATED, event.getTransactionId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentInitiatedEvent for transaction: {}",
                        event.getTransactionId(), ex);
            } else {
                log.debug("PaymentInitiatedEvent published successfully. Topic: {}, Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publishes a PaymentProcessedEvent when payment processing completes (success or failure).
     * Uses the transactionId as the Kafka message key for partition affinity.
     */
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publishing PaymentProcessedEvent for transaction: {}, status: {}",
                event.getTransactionId(), event.getStatus());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_PAYMENT_PROCESSED, event.getTransactionId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentProcessedEvent for transaction: {}",
                        event.getTransactionId(), ex);
            } else {
                log.debug("PaymentProcessedEvent published successfully. Topic: {}, Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
