package com.kafka.learning.chapter02.model;

import java.util.List;
import java.util.Map;

/**
 * Information about the metadata log in KRaft mode.
 *
 * The metadata log (__cluster_metadata) stores all cluster state:
 * - Topic and partition configurations
 * - Broker registrations
 * - ACLs and quotas
 * - Feature flags
 *
 * @param clusterId Unique identifier for the Kafka cluster
 * @param controllerId ID of the current active controller
 * @param brokerCount Number of brokers in the cluster
 * @param topicCount Number of topics (excluding internal)
 * @param totalPartitions Total number of partitions across all topics
 * @param brokers Information about each broker
 */
public record MetadataLogInfo(
        String clusterId,
        int controllerId,
        int brokerCount,
        int topicCount,
        int totalPartitions,
        List<BrokerMetadata> brokers
) {
}
