package com.kafka.learning.common.util;

/**
 * Centralized definition of all Kafka topic names used throughout the learning modules.
 *
 * Why centralize topic names?
 * - Avoids typos and inconsistencies
 * - Makes refactoring easier
 * - Provides documentation of the topic landscape
 * - Enables IDE auto-completion
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Utility class - prevent instantiation
    }

    // ==================== Chapter 1: Architecture ====================
    public static final String ARCHITECTURE_DEMO = "architecture.demo";
    public static final String SEGMENT_EXPLORATION = "segment.exploration";

    // ==================== Chapter 3: Producer ====================
    public static final String PRODUCER_DEMO = "producer.demo";
    public static final String PRODUCER_BATCHING = "producer.batching";
    public static final String PRODUCER_PARTITIONING = "producer.partitioning";
    public static final String PRODUCER_IDEMPOTENT = "producer.idempotent";

    // ==================== Chapter 4: Consumer ====================
    public static final String CONSUMER_DEMO = "consumer.demo";
    public static final String CONSUMER_GROUPS = "consumer.groups";
    public static final String CONSUMER_REBALANCE = "consumer.rebalance";

    // ==================== Chapter 5: Spring Kafka Foundations ====================
    public static final String ORDERS = "orders";
    public static final String ORDER_EVENTS = "order.events";
    public static final String USERS = "users";

    // ==================== Chapter 6: Error Handling ====================
    public static final String PAYMENTS = "payments";
    public static final String PAYMENTS_DLT = "payments.DLT";
    public static final String PAYMENTS_RETRY = "payments.retry";

    // ==================== Chapter 7: Transactions ====================
    public static final String INVENTORY = "inventory";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_FAILED = "order.failed";

    // ==================== Chapter 8: Testing ====================
    public static final String TEST_TOPIC = "test.topic";
    public static final String TEST_ORDERS = "test.orders";

    // ==================== Chapter 9: Event-Driven Architecture ====================
    public static final String ACCOUNT_COMMANDS = "account.commands";
    public static final String ACCOUNT_EVENTS = "account.events";
    public static final String TRANSACTION_SAGA = "transaction.saga";
    public static final String SAGA_REPLY = "saga.reply";

    // ==================== Chapter 10: Kafka Streams ====================
    public static final String PAGE_VIEWS = "page.views";
    public static final String PAGE_VIEW_COUNTS = "page.view.counts";
    public static final String USER_ACTIVITY = "user.activity";
    public static final String USER_SESSIONS = "user.sessions";
    public static final String CLICK_STREAM = "click.stream";
    public static final String ANALYTICS_OUTPUT = "analytics.output";

    // ==================== Chapter 11: Schema Evolution ====================
    public static final String CUSTOMER_V1 = "customer.v1";
    public static final String CUSTOMER_V2 = "customer.v2";
    public static final String PRODUCTS = "products";

    // ==================== Chapter 12: Performance ====================
    public static final String PERF_HIGH_THROUGHPUT = "perf.high-throughput";
    public static final String PERF_LOW_LATENCY = "perf.low-latency";
    public static final String PERF_BENCHMARK = "perf.benchmark";

    // ==================== Chapter 13: Observability ====================
    public static final String METRICS_DEMO = "metrics.demo";
    public static final String TRACING_DEMO = "tracing.demo";

    // ==================== Chapter 14: Security ====================
    public static final String SECURE_TOPIC = "secure.topic";
    public static final String TENANT_A_TOPIC = "tenant-a.topic";
    public static final String TENANT_B_TOPIC = "tenant-b.topic";

    // ==================== Chapter 15: Operations ====================
    public static final String OPS_HEALTH_CHECK = "ops.health-check";
    public static final String OPS_GRACEFUL_SHUTDOWN = "ops.graceful-shutdown";

    // ==================== Dead Letter Topics ====================
    /**
     * Generates a Dead Letter Topic name following the convention: {originalTopic}.DLT
     */
    public static String deadLetterTopic(String originalTopic) {
        return originalTopic + ".DLT";
    }

    /**
     * Generates a retry topic name following the convention: {originalTopic}.retry.{attempt}
     */
    public static String retryTopic(String originalTopic, int attempt) {
        return originalTopic + ".retry." + attempt;
    }
}
