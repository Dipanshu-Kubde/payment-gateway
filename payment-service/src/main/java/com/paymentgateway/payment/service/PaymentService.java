package com.paymentgateway.payment.service;

import com.paymentgateway.common.dto.CreateOrderRequest;
import com.paymentgateway.common.dto.PaymentResponse;
import com.paymentgateway.common.dto.ProcessPaymentRequest;
import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.event.PaymentInitiatedEvent;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import com.paymentgateway.common.exception.DuplicateTransactionException;
import com.paymentgateway.common.exception.PaymentException;
import com.paymentgateway.common.exception.ResourceNotFoundException;
import com.paymentgateway.payment.entity.PaymentOrder;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.kafka.PaymentEventPublisher;
import com.paymentgateway.payment.repository.PaymentOrderRepository;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Core payment processing service.
 * Orchestrates the entire payment lifecycle: order creation, payment initiation,
 * fraud check integration, payment simulation, and event publishing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    // ========================= ORDER MANAGEMENT =========================

    /**
     * Creates a new payment order for a merchant.
     * Enforces idempotency if an idempotencyKey is provided.
     *
     * @param merchantId the merchant creating the order
     * @param request    the order creation request
     * @return PaymentResponse with the generated orderId
     */
    @Transactional
    public PaymentResponse createOrder(Long merchantId, CreateOrderRequest request) {
        log.info("Creating payment order for merchant: {}, amount: {} {}",
                merchantId, request.getAmount(), request.getCurrency());

        // Idempotency check: return cached order if key already exists
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                log.info("Returning existing order for idempotency key: {}", request.getIdempotencyKey());
                return buildOrderResponse(existingOrder.get(), "Order already exists (idempotent)");
            }
        }

        PaymentOrder order = PaymentOrder.builder()
                .merchantId(merchantId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .description(request.getDescription())
                .callbackUrl(request.getCallbackUrl())
                .idempotencyKey(request.getIdempotencyKey())
                .status("CREATED")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        order = orderRepository.save(order);

        log.info("Payment order created successfully. OrderId: {}, MerchantId: {}",
                order.getOrderId(), merchantId);

        return buildOrderResponse(order, "Payment order created successfully");
    }

    // ========================= PAYMENT PROCESSING =========================

    /**
     * Processes a payment against an existing order.
     *
     * Flow:
     * 1. Validate order exists and is not expired
     * 2. Check idempotency via Redis
     * 3. Create PaymentTransaction with INITIATED status
     * 4. Publish PaymentInitiatedEvent to Kafka (triggers fraud detection)
     * 5. Brief wait for fraud result (async, non-blocking in production)
     * 6. Simulate payment processing (gateway interaction)
     * 7. Publish PaymentProcessedEvent to Kafka
     * 8. Return result
     *
     * @param request    the payment processing request
     * @param customerIp the customer's IP address (from request header)
     * @return PaymentResponse with transaction status
     */
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request, String customerIp) {
        log.info("Processing payment for order: {}, method: {}",
                request.getOrderId(), request.getPaymentMethod());

        // Step 1: Validate order
        PaymentOrder order = orderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "orderId", request.getOrderId()));

        if (order.isExpired()) {
            order.setStatus("EXPIRED");
            orderRepository.save(order);
            throw new PaymentException("Payment order has expired: " + request.getOrderId());
        }

        if ("PAID".equals(order.getStatus())) {
            throw new PaymentException("Order has already been paid: " + request.getOrderId());
        }

        // Step 2: Idempotency check via Redis
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            if (idempotencyService.isDuplicate(request.getIdempotencyKey())) {
                String cachedResult = idempotencyService.getCachedResult(request.getIdempotencyKey());
                if (cachedResult != null && !cachedResult.equals("PROCESSING")) {
                    throw new DuplicateTransactionException(request.getIdempotencyKey());
                }
            }
        }

        // Step 3: Create transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .merchantId(order.getMerchantId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.INITIATED)
                .customerEmail(request.getCustomerEmail())
                .customerIp(customerIp)
                .idempotencyKey(request.getIdempotencyKey())
                .riskScore(0)
                .retryCount(0)
                .maxRetries(3)
                .build();

        transaction = transactionRepository.save(transaction);

        // Step 4: Publish PaymentInitiatedEvent (triggers fraud detection asynchronously)
        publishInitiatedEvent(transaction, request);

        // Step 5: Brief wait for fraud check (simulated async coordination)
        waitForFraudCheck(transaction.getTransactionId());

        // Step 6: Simulate payment processing
        simulatePayment(transaction);

        // Step 7: Update order status if payment succeeded
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            order.setStatus("PAID");
            orderRepository.save(order);
        }

        transaction = transactionRepository.save(transaction);

        // Step 8: Publish PaymentProcessedEvent
        publishProcessedEvent(transaction);

        // Store idempotency result
        if (request.getIdempotencyKey() != null) {
            idempotencyService.storeResult(request.getIdempotencyKey(), transaction.getStatus().name());
        }

        log.info("Payment processing completed. TransactionId: {}, Status: {}",
                transaction.getTransactionId(), transaction.getStatus());

        return buildTransactionResponse(transaction);
    }

    // ========================= PAYMENT SIMULATION =========================

    /**
     * Simulates payment gateway interaction.
     * Distribution: 85% SUCCESS, 10% FAILED, 5% TIMEOUT (RETRY_SCHEDULED)
     */
    public void simulatePayment(PaymentTransaction transaction) {
        log.info("Simulating payment for transaction: {}", transaction.getTransactionId());

        // If transaction was blocked by fraud check, skip simulation
        if (transaction.getStatus() == TransactionStatus.FRAUD_REVIEW) {
            log.warn("Transaction {} is under fraud review, skipping payment simulation",
                    transaction.getTransactionId());
            return;
        }

        transaction.setStatus(TransactionStatus.PROCESSING);

        // Simulate processing delay (50-200ms)
        try {
            Thread.sleep(50 + random.nextInt(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int outcome = random.nextInt(100);

        if (outcome < 85) {
            // 85% - Success
            transaction.setStatus(TransactionStatus.SUCCESS);
            log.info("Payment SUCCEEDED for transaction: {}", transaction.getTransactionId());

        } else if (outcome < 95) {
            // 10% - Failure
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(getRandomFailureReason());
            log.warn("Payment FAILED for transaction: {}. Reason: {}",
                    transaction.getTransactionId(), transaction.getFailureReason());

        } else {
            // 5% - Timeout, schedule retry
            transaction.setStatus(TransactionStatus.RETRY_SCHEDULED);
            transaction.setFailureReason("Payment gateway timeout");
            transaction.setNextRetryAt(LocalDateTime.now().plusSeconds(transaction.getNextRetryDelaySeconds()));
            log.warn("Payment TIMED OUT for transaction: {}. Retry scheduled at: {}",
                    transaction.getTransactionId(), transaction.getNextRetryAt());
        }
    }

    // ========================= ORDER STATUS =========================

    /**
     * Retrieves order details with all associated transactions.
     *
     * @param orderId the order ID to look up
     * @return PaymentResponse with order status
     */
    @Transactional(readOnly = true)
    public PaymentResponse getOrderStatus(String orderId) {
        PaymentOrder order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "orderId", orderId));

        // Check if expired
        if ("CREATED".equals(order.getStatus()) && order.isExpired()) {
            order.setStatus("EXPIRED");
            orderRepository.save(order);
        }

        return buildOrderResponse(order, "Order retrieved successfully");
    }

    /**
     * Retrieves all orders for a given merchant.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMerchantOrders(Long merchantId) {
        return orderRepository.findByMerchantId(merchantId).stream()
                .map(order -> buildOrderResponse(order, null))
                .toList();
    }

    /**
     * Retrieves all transactions for a given order.
     */
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getOrderTransactions(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    // ========================= RETRY =========================

    /**
     * Manually retries a failed or timed-out transaction.
     * Enforces exponential backoff and max retry limits.
     *
     * @param transactionId the transaction to retry
     * @return PaymentResponse with the retry result
     */
    @Transactional
    public PaymentResponse retryPayment(String transactionId) {
        log.info("Manual retry requested for transaction: {}", transactionId);

        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "transactionId", transactionId));

        if (!transaction.canRetry()) {
            throw new PaymentException(String.format(
                    "Transaction %s cannot be retried. Status: %s, Retries: %d/%d",
                    transactionId, transaction.getStatus(),
                    transaction.getRetryCount(), transaction.getMaxRetries()));
        }

        // Increment retry count
        transaction.setRetryCount(transaction.getRetryCount() + 1);
        transaction.setFailureReason(null);

        // Simulate payment again
        simulatePayment(transaction);

        // If still failing and retries exhausted, mark as permanently failed
        if (transaction.getStatus() == TransactionStatus.FAILED && !transaction.canRetry()) {
            transaction.setFailureReason("Max retries exhausted. Last failure: " + transaction.getFailureReason());
            log.error("Transaction {} permanently failed after {} retries",
                    transactionId, transaction.getRetryCount());
        }

        // Update order status if payment succeeded
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            orderRepository.findByOrderId(transaction.getOrderId())
                    .ifPresent(order -> {
                        order.setStatus("PAID");
                        orderRepository.save(order);
                    });
        }

        transaction = transactionRepository.save(transaction);

        // Publish processed event
        publishProcessedEvent(transaction);

        return buildTransactionResponse(transaction);
    }

    // ========================= PRIVATE HELPERS =========================

    private void publishInitiatedEvent(PaymentTransaction transaction, ProcessPaymentRequest request) {
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId(transaction.getTransactionId())
                .orderId(transaction.getOrderId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .customerEmail(request.getCustomerEmail())
                .customerIp(transaction.getCustomerIp())
                .cardBinCountry(extractCardBinCountry(request))
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPaymentInitiated(event);
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

    /**
     * Brief wait to allow asynchronous fraud check to complete.
     * In production, this would use a CompletableFuture or event-driven pattern.
     */
    private void waitForFraudCheck(String transactionId) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Fraud check wait interrupted for transaction: {}", transactionId);
        }

        // Re-read the transaction to get any fraud check updates
        transactionRepository.findByTransactionId(transactionId)
                .ifPresent(txn -> {
                    if (txn.getStatus() == TransactionStatus.FRAUD_REVIEW) {
                        log.warn("Transaction {} flagged during fraud check", transactionId);
                    }
                });
    }

    /**
     * Extracts the card BIN country from the card number (first 6 digits).
     * Simplified: returns "IN" for simulation purposes.
     */
    private String extractCardBinCountry(ProcessPaymentRequest request) {
        if (request.getCardNumber() != null && request.getCardNumber().length() >= 6) {
            // In production, look up BIN database
            return "IN";
        }
        return null;
    }

    private String getRandomFailureReason() {
        String[] reasons = {
                "Insufficient funds",
                "Card declined by issuer",
                "Invalid card details",
                "Bank server unavailable",
                "Transaction limit exceeded",
                "3D Secure authentication failed"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    private PaymentResponse buildOrderResponse(PaymentOrder order, String message) {
        return PaymentResponse.builder()
                .orderId(order.getOrderId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(mapOrderStatus(order.getStatus()))
                .message(message)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private PaymentResponse buildTransactionResponse(PaymentTransaction transaction) {
        return PaymentResponse.builder()
                .orderId(transaction.getOrderId())
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .message(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Maps order status string to TransactionStatus for consistent API responses.
     */
    private TransactionStatus mapOrderStatus(String orderStatus) {
        return switch (orderStatus) {
            case "CREATED" -> TransactionStatus.CREATED;
            case "PAID" -> TransactionStatus.SUCCESS;
            case "EXPIRED" -> TransactionStatus.FAILED;
            default -> TransactionStatus.CREATED;
        };
    }
}
