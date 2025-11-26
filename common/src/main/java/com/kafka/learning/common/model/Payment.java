package com.kafka.learning.common.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment model used in error handling and transaction chapters.
 */
public record Payment(
        String paymentId,
        String orderId,
        String customerId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant processedAt
) {

    public Payment {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    /**
     * Factory method for creating a new payment.
     */
    public static Payment create(String paymentId, String orderId, String customerId,
                                  BigDecimal amount, PaymentMethod method) {
        return new Payment(
                paymentId,
                orderId,
                customerId,
                amount,
                method,
                PaymentStatus.PENDING,
                null,
                Instant.now(),
                null
        );
    }

    /**
     * Creates a copy marked as successful.
     */
    public Payment succeeded() {
        return new Payment(
                paymentId,
                orderId,
                customerId,
                amount,
                method,
                PaymentStatus.COMPLETED,
                null,
                createdAt,
                Instant.now()
        );
    }

    /**
     * Creates a copy marked as failed with reason.
     */
    public Payment failed(String reason) {
        return new Payment(
                paymentId,
                orderId,
                customerId,
                amount,
                method,
                PaymentStatus.FAILED,
                reason,
                createdAt,
                Instant.now()
        );
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        BANK_TRANSFER,
        PAYPAL,
        CRYPTO
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
