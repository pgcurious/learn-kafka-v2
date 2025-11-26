package com.kafka.learning.chapter03.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Advanced producer service demonstrating various producer patterns.
 *
 * This service shows:
 * - Async sending with callbacks
 * - Batch sending with timing
 * - Performance benchmarking
 * - Error handling patterns
 */
@Service
public class AdvancedProducerService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public AdvancedProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a message asynchronously with proper error handling.
     *
     * Best practice: Always handle the future to catch send failures.
     * Fire-and-forget sending can silently lose messages.
     */
    public CompletableFuture<SendResult<String, String>> sendAsync(
            String topic, String key, String value) {

        log.debug("Sending message to topic={}, key={}", topic, key);

        return kafkaTemplate.send(topic, key, value)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to {}: {}", topic, ex.getMessage());
                    } else {
                        RecordMetadata metadata = result.getRecordMetadata();
                        log.debug("Sent to partition={}, offset={}, timestamp={}",
                                metadata.partition(),
                                metadata.offset(),
                                metadata.timestamp());
                    }
                });
    }

    /**
     * Sends multiple messages and waits for all to complete.
     *
     * This pattern is useful when you need to ensure a batch of
     * related messages are all sent before proceeding.
     */
    public BatchSendResult sendBatch(String topic, List<KeyValue> messages) {
        Instant start = Instant.now();

        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (KeyValue kv : messages) {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, kv.key(), kv.value())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            failureCount.incrementAndGet();
                        } else {
                            successCount.incrementAndGet();
                        }
                    });
            futures.add(future);
        }

        // Wait for all sends to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Duration duration = Duration.between(start, Instant.now());

        return new BatchSendResult(
                messages.size(),
                successCount.get(),
                failureCount.get(),
                duration.toMillis()
        );
    }

    /**
     * Runs a throughput benchmark.
     *
     * This method sends a specified number of messages and measures:
     * - Total time
     * - Messages per second
     * - Latency statistics
     */
    public BenchmarkResult runBenchmark(String topic, int messageCount, int messageSize) {
        log.info("Starting benchmark: {} messages of {} bytes", messageCount, messageSize);

        String payload = "x".repeat(messageSize);
        List<Long> latencies = new ArrayList<>(messageCount);
        LongAdder totalBytes = new LongAdder();

        Instant start = Instant.now();

        List<CompletableFuture<Void>> futures = new ArrayList<>(messageCount);

        for (int i = 0; i < messageCount; i++) {
            String key = "key-" + (i % 100);
            Instant sendStart = Instant.now();

            CompletableFuture<Void> future = kafkaTemplate.send(topic, key, payload)
                    .thenAccept(result -> {
                        long latency = Duration.between(sendStart, Instant.now()).toMillis();
                        synchronized (latencies) {
                            latencies.add(latency);
                        }
                        totalBytes.add(messageSize);
                    })
                    .exceptionally(ex -> {
                        log.error("Send failed: {}", ex.getMessage());
                        return null;
                    });

            futures.add(future);
        }

        // Wait for all sends
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Ensure all messages are flushed
        kafkaTemplate.flush();

        Duration totalDuration = Duration.between(start, Instant.now());

        // Calculate statistics
        LongSummaryStatistics stats = latencies.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        double messagesPerSecond = messageCount / (totalDuration.toMillis() / 1000.0);
        double mbPerSecond = (totalBytes.sum() / 1024.0 / 1024.0) / (totalDuration.toMillis() / 1000.0);

        // Calculate percentiles
        List<Long> sorted = latencies.stream().sorted().toList();
        long p50 = sorted.get((int) (sorted.size() * 0.50));
        long p95 = sorted.get((int) (sorted.size() * 0.95));
        long p99 = sorted.get((int) (sorted.size() * 0.99));

        BenchmarkResult result = new BenchmarkResult(
                messageCount,
                totalDuration.toMillis(),
                messagesPerSecond,
                mbPerSecond,
                stats.getAverage(),
                p50,
                p95,
                p99
        );

        log.info("Benchmark complete: {}", result);
        return result;
    }

    /**
     * Sends a message with explicit partition assignment.
     *
     * Use this when you need precise control over which partition
     * receives the message (rare in practice).
     */
    public CompletableFuture<SendResult<String, String>> sendToPartition(
            String topic, int partition, String key, String value) {

        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic, partition, key, value);

        return kafkaTemplate.send(record);
    }

    // Record types for results
    public record KeyValue(String key, String value) {}

    public record BatchSendResult(
            int total,
            int succeeded,
            int failed,
            long durationMs
    ) {}

    public record BenchmarkResult(
            int messageCount,
            long totalMs,
            double messagesPerSecond,
            double mbPerSecond,
            double avgLatencyMs,
            long p50LatencyMs,
            long p95LatencyMs,
            long p99LatencyMs
    ) {}
}
