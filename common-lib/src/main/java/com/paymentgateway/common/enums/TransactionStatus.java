package com.paymentgateway.common.enums;

public enum TransactionStatus {
    CREATED,
    INITIATED,
    PROCESSING,
    SUCCESS,
    FAILED,
    RETRY_SCHEDULED,
    FRAUD_REVIEW,
    APPROVED,
    REJECTED,
    REFUND_INITIATED,
    REFUNDED
}
