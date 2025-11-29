package com.kafka.learning.chapter02.model;

import java.util.List;

/**
 * Response model for KRaft quorum information.
 *
 * The quorum consists of controller nodes that participate in the
 * Raft consensus protocol for metadata management.
 *
 * @param leaderId The current leader of the controller quorum
 * @param leaderEpoch The epoch (term) of the current leader
 * @param highWatermark The committed offset in the metadata log
 * @param voters List of voter replicas (can vote in elections)
 * @param observers List of observer replicas (non-voting)
 */
public record QuorumInfoResponse(
        int leaderId,
        long leaderEpoch,
        long highWatermark,
        List<ReplicaState> voters,
        List<ReplicaState> observers
) {
}
