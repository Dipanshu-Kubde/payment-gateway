package com.paymentgateway.common.enums;

public enum RiskLevel {
    LOW,        // Score 0-30: Auto-approve
    MEDIUM,     // Score 31-60: Flag for review
    HIGH,       // Score 61-80: Block, require manual approval
    CRITICAL    // Score 81-100: Auto-block, alert fraud team
}
