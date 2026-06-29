package com.paymentgateway.merchant.entity;

import com.paymentgateway.common.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchant_email", columnList = "email"),
        @Index(name = "idx_merchant_api_key_hash", columnList = "apiKeyHash"),
        @Index(name = "idx_merchant_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Plaintext API key (stored temporarily for display, but kept hashed for validation).
     * In production, consider NOT storing plaintext — only store the hash.
     * Here we store it for lookup convenience; real Stripe-like systems hash it.
     */
    @Column(name = "api_key", unique = true, length = 255)
    private String apiKey;

    @Column(name = "api_key_hash", length = 255)
    private String apiKeyHash;

    @Column(name = "api_secret", length = 255)
    private String apiSecret;

    @Column(name = "api_secret_hash", length = 255)
    private String apiSecretHash;

    @Column(name = "webhook_url", length = 512)
    private String webhookUrl;

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal feePercentage = new BigDecimal("2.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
