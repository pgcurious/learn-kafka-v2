package com.kafka.learning.chapter01.model;

import java.util.List;
import java.util.Map;

/**
 * Detailed information about a Kafka topic.
 *
 * @param name              Topic name
 * @param partitionCount    Number of partitions
 * @param replicationFactor Number of replicas per partition
 * @param internal          Whether this is an internal Kafka topic
 * @param partitions        Details for each partition
 * @param configs           Topic configuration (retention, segment size, etc.)
 */
public record TopicDetails(
        String name,
        int partitionCount,
        int replicationFactor,
        boolean internal,
        List<PartitionDetails> partitions,
        Map<String, String> configs
) {
}
