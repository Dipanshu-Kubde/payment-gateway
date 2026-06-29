package com.paymentgateway.fraud.kafka;

import com.paymentgateway.common.event.FraudCheckResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes fraud check results to the fraud.check.result Kafka topic.
 * Consumed by Payment Service to make approve/block decisions.
 */
@Component
@Slf4j
public class FraudEventPublisher {

    private static final String TOPIC = "fraud.check.result";

    private final KafkaTemplate<String, FraudCheckResultEvent> kafkaTemplate;

    public FraudEventPublisher(KafkaTemplate<String, FraudCheckResultEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishFraudResult(FraudCheckResultEvent event) {
        log.info("Publishing fraud.check.result for transaction: {}, score: {}, blocked: {}",
                event.getTransactionId(), event.getRiskScore(), event.isBlocked());

        kafkaTemplate.send(TOPIC, event.getTransactionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud result for {}: {}",
                                event.getTransactionId(), ex.getMessage());
                    } else {
                        log.info("Fraud result published successfully for txn {}",
                                event.getTransactionId());
                    }
                });
    }
}
