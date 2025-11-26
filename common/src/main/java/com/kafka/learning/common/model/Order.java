package com.kafka.learning.common.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Order model used across multiple chapters.
 *
 * Using a record (Java 16+) for immutable data carriers.
 * Records automatically provide:
 * - Constructor with all fields
 * - Getter methods (without 'get' prefix)
 * - equals(), hashCode(), toString()
 * - Compact, readable syntax
 */
public record Order(
        String orderId,
        String customerId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Compact constructor for validation.
     */
    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId cannot be null or blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be null or empty");
        }
        // Make items immutable
        items = List.copyOf(items);
    }

    /**
     * Factory method for creating a new order.
     */
    public static Order create(String orderId, String customerId, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Order(
                orderId,
                customerId,
                items,
                total,
                OrderStatus.PENDING,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Creates a copy with updated status.
     */
    public Order withStatus(OrderStatus newStatus) {
        return new Order(
                orderId,
                customerId,
                items,
                totalAmount,
                newStatus,
                createdAt,
                Instant.now()
        );
    }

    public record OrderItem(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {
        public OrderItem {
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("unitPrice must be non-negative");
            }
        }
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PAID,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }
}
