package com.kafka.learning.chapter02.model;

/**
 * Represents the state of a replica in the controller quorum.
 *
 * In KRaft mode, each controller maintains a copy of the metadata log.
 * This record captures the replication state of each controller.
 *
 * @param replicaId Unique identifier of the controller node
 * @param logEndOffset The last offset in this replica's log
 * @param lastFetchTimestamp When this replica last fetched from the leader (ms since epoch)
 * @param lastCaughtUpTimestamp When this replica was last fully caught up (ms since epoch)
 */
public record ReplicaState(
        int replicaId,
        long logEndOffset,
        long lastFetchTimestamp,
        long lastCaughtUpTimestamp
) {
}
