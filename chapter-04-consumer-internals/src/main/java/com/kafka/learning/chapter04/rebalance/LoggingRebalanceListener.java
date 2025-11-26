package com.kafka.learning.chapter04.rebalance;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A rebalance listener that logs partition assignments and tracks statistics.
 *
 * This implementation demonstrates:
 * - Proper handling of partition assignment/revocation
 * - Tracking rebalance events for monitoring
 * - Offset commit before partition revocation (important for at-least-once)
 *
 * In production, you would extend this to:
 * - Save processing state before revocation
 * - Restore state after assignment
 * - Integrate with monitoring systems
 */
public class LoggingRebalanceListener implements ConsumerRebalanceListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingRebalanceListener.class);

    private final Consumer<?, ?> consumer;
    private final AtomicInteger rebalanceCount = new AtomicInteger(0);
    private final Map<TopicPartition, Long> partitionAssignmentTime = new ConcurrentHashMap<>();

    public LoggingRebalanceListener(Consumer<?, ?> consumer) {
        this.consumer = consumer;
    }

    /**
     * Called when partitions are about to be revoked.
     *
     * IMPORTANT: This is your last chance to:
     * - Commit any pending offsets
     * - Save any in-memory state
     * - Clean up partition-specific resources
     *
     * After this method returns, you no longer own these partitions.
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions REVOKED: {} (rebalance #{})",
                formatPartitions(partitions), rebalanceCount.get());

        if (partitions.isEmpty()) {
            return;
        }

        // Best practice: Commit offsets synchronously before revocation
        // This ensures we don't lose track of processed messages
        try {
            log.info("Committing offsets before partition revocation...");
            consumer.commitSync();
            log.info("Offsets committed successfully");
        } catch (Exception e) {
            log.error("Failed to commit offsets during revocation", e);
            // In production, you might want to handle this differently
            // depending on your delivery guarantees
        }

        // Track how long we held each partition (useful for debugging)
        for (TopicPartition partition : partitions) {
            Long assignedAt = partitionAssignmentTime.remove(partition);
            if (assignedAt != null) {
                long durationMs = System.currentTimeMillis() - assignedAt;
                log.info("Partition {} was held for {} ms", partition, durationMs);
            }
        }
    }

    /**
     * Called when new partitions are assigned.
     *
     * This is the place to:
     * - Initialize partition-specific state
     * - Load checkpoints or state from external storage
     * - Set up partition-specific resources
     */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        rebalanceCount.incrementAndGet();

        log.info("Partitions ASSIGNED: {} (rebalance #{})",
                formatPartitions(partitions), rebalanceCount.get());

        // Track assignment time
        long now = System.currentTimeMillis();
        for (TopicPartition partition : partitions) {
            partitionAssignmentTime.put(partition, now);
        }

        // Log current positions (useful for debugging)
        for (TopicPartition partition : partitions) {
            try {
                long position = consumer.position(partition);
                log.info("Starting position for {}: {}", partition, position);
            } catch (Exception e) {
                log.warn("Could not get position for {}: {}", partition, e.getMessage());
            }
        }
    }

    /**
     * Called when partitions are lost (cooperative rebalancing only).
     *
     * Unlike onPartitionsRevoked(), this is called when partitions are
     * removed without a clean revocation - typically due to:
     * - Consumer being too slow (max.poll.interval.ms exceeded)
     * - Network issues causing heartbeat failures
     *
     * WARNING: By the time this is called, another consumer may already
     * be processing these partitions. Any pending state is potentially stale.
     */
    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        log.warn("Partitions LOST (not cleanly revoked): {}",
                formatPartitions(partitions));

        // DON'T commit offsets here - another consumer may have already
        // committed newer offsets

        // Clean up state, but mark it as potentially stale
        for (TopicPartition partition : partitions) {
            partitionAssignmentTime.remove(partition);
            log.warn("State for {} may be inconsistent - another consumer may have taken over",
                    partition);
        }
    }

    /**
     * Returns the number of rebalances that have occurred.
     */
    public int getRebalanceCount() {
        return rebalanceCount.get();
    }

    /**
     * Returns the currently assigned partitions with their assignment duration.
     */
    public Map<TopicPartition, Long> getPartitionAssignmentTimes() {
        return Map.copyOf(partitionAssignmentTime);
    }

    private String formatPartitions(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty()) {
            return "[]";
        }
        return partitions.stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .sorted()
                .toList()
                .toString();
    }
}
