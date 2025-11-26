package com.kafka.learning.chapter03.partitioner;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom partitioner that routes messages based on priority.
 *
 * Use case: Route high-priority messages to a dedicated partition (partition 0)
 * so they can be processed by dedicated consumers with higher priority.
 *
 * How it works:
 * 1. If the key contains "HIGH" or "URGENT", route to partition 0
 * 2. Otherwise, use standard key-based or random partitioning
 *
 * Configuration in application.yml:
 * spring.kafka.producer.properties.partitioner.class=com.kafka.learning.chapter03.partitioner.PriorityPartitioner
 */
public class PriorityPartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(PriorityPartitioner.class);
    private static final int PRIORITY_PARTITION = 0;

    @Override
    public void configure(Map<String, ?> configs) {
        log.info("PriorityPartitioner configured with: {}", configs);
    }

    /**
     * Determines which partition a record should be sent to.
     *
     * @param topic      The topic name
     * @param key        The key (may be null)
     * @param keyBytes   Serialized key bytes
     * @param value      The value
     * @param valueBytes Serialized value bytes
     * @param cluster    Current cluster metadata
     * @return The partition number to send to
     */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {

        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (numPartitions == 0) {
            throw new IllegalArgumentException("Topic " + topic + " has no partitions");
        }

        // Check for priority in key
        if (isPriorityMessage(key)) {
            log.debug("Routing priority message to partition {}", PRIORITY_PARTITION);
            return PRIORITY_PARTITION;
        }

        // Standard partitioning for non-priority messages
        if (keyBytes == null) {
            // No key - use random partition (excluding priority partition if > 1 partition)
            if (numPartitions > 1) {
                return 1 + ThreadLocalRandom.current().nextInt(numPartitions - 1);
            }
            return 0;
        }

        // Key-based partitioning using murmur2 hash
        // This ensures same key always goes to same partition
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
    }

    /**
     * Checks if a message should be treated as high priority.
     */
    private boolean isPriorityMessage(Object key) {
        if (key == null) {
            return false;
        }

        String keyStr = key.toString().toUpperCase();
        return keyStr.contains("HIGH") ||
               keyStr.contains("URGENT") ||
               keyStr.contains("CRITICAL");
    }

    @Override
    public void close() {
        log.info("PriorityPartitioner closed");
    }
}
