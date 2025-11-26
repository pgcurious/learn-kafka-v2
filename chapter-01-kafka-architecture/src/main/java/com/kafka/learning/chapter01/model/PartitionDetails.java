package com.kafka.learning.chapter01.model;

import java.util.List;

/**
 * Details about a specific partition.
 *
 * @param partition Partition number (0-based)
 * @param leader    Broker ID of the leader (-1 if no leader)
 * @param replicas  List of broker IDs holding replicas
 * @param isr       In-Sync Replicas - replicas caught up with leader
 */
public record PartitionDetails(
        int partition,
        int leader,
        List<Integer> replicas,
        List<Integer> isr
) {
    /**
     * Checks if all replicas are in sync.
     */
    public boolean isFullyReplicated() {
        return replicas.size() == isr.size();
    }

    /**
     * Returns the number of replicas not in sync.
     */
    public int outOfSyncReplicaCount() {
        return replicas.size() - isr.size();
    }
}
