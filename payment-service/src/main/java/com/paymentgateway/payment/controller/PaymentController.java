package com.paymentgateway.payment.controller;

import com.paymentgateway.common.dto.ApiResponse;
import com.paymentgateway.common.dto.CreateOrderRequest;
import com.paymentgateway.common.dto.PaymentResponse;
import com.paymentgateway.common.dto.ProcessPaymentRequest;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing payment processing endpoints.
 * All endpoints return the standard ApiResponse wrapper from common-lib.
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment order for a merchant.
     *
     * POST /api/payments/orders
     * Header: X-Merchant-Id (required)
     */
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PaymentResponse>> createOrder(
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("REST: Create order request from merchant: {}", merchantId);
        PaymentResponse response = paymentService.createOrder(merchantId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment order created successfully", response));
    }

    /**
     * Processes a payment against an existing order.
     *
     * POST /api/payments/process
     * Automatically extracts customer IP from the request.
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            HttpServletRequest httpRequest) {

        String customerIp = extractClientIp(httpRequest);
        log.info("REST: Process payment for order: {}, IP: {}", request.getOrderId(), customerIp);

        PaymentResponse response = paymentService.processPayment(request, customerIp);

        return ResponseEntity.ok(ApiResponse.success("Payment processed", response));
    }

    /**
     * Retrieves the status of a payment order.
     *
     * GET /api/payments/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getOrderStatus(
            @PathVariable String orderId) {

        log.info("REST: Get order status for: {}", orderId);
        PaymentResponse response = paymentService.getOrderStatus(orderId);

        return ResponseEntity.ok(ApiResponse.success("Order status retrieved", response));
    }

    /**
     * Retrieves all transactions for a specific order.
     *
     * GET /api/payments/orders/{orderId}/transactions
     */
    @GetMapping("/orders/{orderId}/transactions")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getOrderTransactions(
            @PathVariable String orderId) {

        log.info("REST: Get transactions for order: {}", orderId);
        List<PaymentTransaction> transactions = paymentService.getOrderTransactions(orderId);

        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", transactions));
    }

    /**
     * Retries a failed or timed-out payment transaction.
     *
     * POST /api/payments/{transactionId}/retry
     */
    @PostMapping("/{transactionId}/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @PathVariable String transactionId) {

        log.info("REST: Retry payment for transaction: {}", transactionId);
        PaymentResponse response = paymentService.retryPayment(transactionId);

        return ResponseEntity.ok(ApiResponse.success("Payment retry processed", response));
    }

    /**
     * Retrieves all orders for a specific merchant.
     *
     * GET /api/payments/merchant/{merchantId}
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMerchantOrders(
            @PathVariable Long merchantId) {

        log.info("REST: Get orders for merchant: {}", merchantId);
        List<PaymentResponse> orders = paymentService.getMerchantOrders(merchantId);

        return ResponseEntity.ok(ApiResponse.success("Merchant orders retrieved", orders));
    }

    /**
     * Extracts the real client IP, accounting for reverse proxies and load balancers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
