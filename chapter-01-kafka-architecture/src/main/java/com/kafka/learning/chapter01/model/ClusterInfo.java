package com.kafka.learning.chapter01.model;

import java.util.List;

/**
 * Represents Kafka cluster information.
 *
 * @param clusterId   Unique identifier for the cluster
 * @param controllerId ID of the controller broker (handles metadata operations)
 * @param brokers     List of all brokers in the cluster
 */
public record ClusterInfo(
        String clusterId,
        int controllerId,
        List<BrokerInfo> brokers
) {
}
