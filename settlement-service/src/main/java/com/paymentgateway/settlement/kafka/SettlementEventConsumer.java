package com.paymentgateway.settlement.kafka;

import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import com.paymentgateway.settlement.entity.MerchantBalance;
import com.paymentgateway.settlement.repository.MerchantBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Consumes payment.processed events from Kafka.
 * When a payment succeeds, the merchant's balance is updated:
 *   - totalEarnings += amount
 *   - pendingAmount += amount
 * Creates a new MerchantBalance record if none exists for the merchant.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventConsumer {

    private final MerchantBalanceRepository merchantBalanceRepository;

    @KafkaListener(
            topics = "payment.processed",
            groupId = "settlement-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment.processed event: transactionId={}, merchantId={}, amount={}, status={}",
                event.getTransactionId(), event.getMerchantId(), event.getAmount(), event.getStatus());

        // Only process successful payments for settlement
        if (event.getStatus() != TransactionStatus.SUCCESS) {
            log.info("Skipping non-successful payment: transactionId={}, status={}",
                    event.getTransactionId(), event.getStatus());
            return;
        }

        try {
            MerchantBalance balance = merchantBalanceRepository
                    .findByMerchantId(event.getMerchantId())
                    .orElseGet(() -> {
                        log.info("Creating new MerchantBalance record for merchantId={}",
                                event.getMerchantId());
                        return MerchantBalance.builder()
                                .merchantId(event.getMerchantId())
                                .totalEarnings(BigDecimal.ZERO)
                                .settledAmount(BigDecimal.ZERO)
                                .pendingAmount(BigDecimal.ZERO)
                                .build();
                    });

            BigDecimal amount = event.getAmount();
            balance.setTotalEarnings(balance.getTotalEarnings().add(amount));
            balance.setPendingAmount(balance.getPendingAmount().add(amount));

            merchantBalanceRepository.save(balance);

            log.info("Updated merchant balance: merchantId={}, totalEarnings={}, pendingAmount={}",
                    event.getMerchantId(), balance.getTotalEarnings(), balance.getPendingAmount());

        } catch (Exception e) {
            log.error("Error processing payment event for merchantId={}: {}",
                    event.getMerchantId(), e.getMessage(), e);
            throw e; // rethrow to trigger Kafka retry / DLQ
        }
    }
}
