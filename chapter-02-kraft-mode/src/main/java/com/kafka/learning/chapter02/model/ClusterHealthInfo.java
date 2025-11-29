package com.kafka.learning.chapter02.model;

import java.util.List;

/**
 * Health information about the KRaft cluster.
 *
 * Provides an overview of the cluster's operational state,
 * useful for monitoring and troubleshooting.
 *
 * @param healthy Overall cluster health status
 * @param controllerId Active controller ID
 * @param quorumSize Number of controllers in the quorum
 * @param voterCount Number of voting members
 * @param observerCount Number of observer members
 * @param underReplicatedPartitions Count of under-replicated partitions
 * @param offlinePartitions Count of offline partitions
 * @param issues List of any detected issues
 */
public record ClusterHealthInfo(
        boolean healthy,
        int controllerId,
        int quorumSize,
        int voterCount,
        int observerCount,
        int underReplicatedPartitions,
        int offlinePartitions,
        List<String> issues
) {
}
