package com.paymentgateway.common.dto;

import com.paymentgateway.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String customerEmail;

    private String customerPhone;

    // Card details (simulated)
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;

    // UPI details
    private String upiId;

    // Net banking
    private String bankCode;

    private String idempotencyKey;
}
