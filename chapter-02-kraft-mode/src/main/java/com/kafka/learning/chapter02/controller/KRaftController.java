package com.kafka.learning.chapter02.controller;

import com.kafka.learning.chapter02.model.*;
import com.kafka.learning.chapter02.service.KRaftMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for exploring KRaft mode.
 *
 * Provides endpoints to query KRaft-specific information:
 * - Controller quorum state
 * - Metadata log information
 * - Cluster health
 * - Feature flags
 *
 * Endpoints:
 * - GET  /api/kraft/quorum-info     - Controller quorum information
 * - GET  /api/kraft/controller      - Active controller details
 * - GET  /api/kraft/metadata-log    - Metadata log summary
 * - GET  /api/kraft/health          - Cluster health check
 * - GET  /api/kraft/features        - Feature flags
 * - GET  /api/kraft/brokers         - List all brokers
 */
@RestController
@RequestMapping("/api/kraft")
public class KRaftController {

    private static final Logger log = LoggerFactory.getLogger(KRaftController.class);

    private final KRaftMetadataService kraftMetadataService;

    public KRaftController(KRaftMetadataService kraftMetadataService) {
        this.kraftMetadataService = kraftMetadataService;
    }

    /**
     * Get controller quorum information.
     *
     * Returns information about the KRaft controller quorum including:
     * - Current leader and epoch
     * - High watermark (committed offset)
     * - Voter and observer states
     *
     * This is the KRaft equivalent of checking ZooKeeper's /controller znode
     * in legacy Kafka deployments.
     *
     * Example response:
     * {
     *   "leaderId": 1,
     *   "leaderEpoch": 5,
     *   "highWatermark": 1234,
     *   "voters": [...],
     *   "observers": []
     * }
     */
    @GetMapping("/quorum-info")
    public ResponseEntity<QuorumInfoResponse> getQuorumInfo() {
        try {
            QuorumInfoResponse info = kraftMetadataService.getQuorumInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get quorum info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get active controller information.
     *
     * Returns details about the current active controller (quorum leader).
     * The active controller is responsible for:
     * - Processing metadata changes (topic creation, etc.)
     * - Replicating metadata to followers
     * - Coordinating broker registrations
     */
    @GetMapping("/controller")
    public ResponseEntity<ControllerInfo> getController() {
        try {
            ControllerInfo info = kraftMetadataService.getControllerInfo();
            return ResponseEntity.ok(info);
        } catch (IllegalStateException e) {
            log.warn("Controller not available: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get controller info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get metadata log information.
     *
     * The metadata log (__cluster_metadata) is the heart of KRaft mode.
     * It stores all cluster state as a sequence of records:
     * - TopicRecord: Topic creations and deletions
     * - PartitionRecord: Partition assignments
     * - BrokerRegistrationRecord: Broker lifecycle events
     * - ConfigRecord: Configuration changes
     *
     * This endpoint provides a summary of the metadata state.
     */
    @GetMapping("/metadata-log")
    public ResponseEntity<MetadataLogInfo> getMetadataLog() {
        try {
            MetadataLogInfo info = kraftMetadataService.getMetadataLogInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get metadata log info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get cluster health information.
     *
     * Performs health checks on the KRaft cluster:
     * - Controller availability
     * - Quorum voter states
     * - Partition health (under-replicated, offline)
     *
     * Use this endpoint for monitoring and alerting.
     */
    @GetMapping("/health")
    public ResponseEntity<ClusterHealthInfo> getHealth() {
        try {
            ClusterHealthInfo health = kraftMetadataService.getClusterHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Failed to check cluster health", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get feature flags.
     *
     * KRaft introduces versioned features that can be upgraded independently.
     * Features include:
     * - metadata.version: Version of the metadata format
     * - transaction.version: Transaction protocol version
     * - group.version: Consumer group protocol version
     *
     * These can be upgraded without a full cluster restart.
     */
    @GetMapping("/features")
    public ResponseEntity<Map<String, String>> getFeatures() {
        try {
            Map<String, String> features = kraftMetadataService.getFeatureFlags();
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            log.error("Failed to get feature flags", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * List all brokers in the cluster.
     *
     * Returns information about all brokers including:
     * - Broker ID and endpoints
     * - Rack assignment
     * - Whether the broker is the active controller
     */
    @GetMapping("/brokers")
    public ResponseEntity<List<BrokerMetadata>> listBrokers() {
        try {
            List<BrokerMetadata> brokers = kraftMetadataService.listBrokers();
            return ResponseEntity.ok(brokers);
        } catch (Exception e) {
            log.error("Failed to list brokers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get just the controller ID.
     *
     * Lightweight endpoint that returns only the active controller's broker ID.
     * Useful for quick checks in scripts or monitoring.
     */
    @GetMapping("/controller-id")
    public ResponseEntity<Map<String, Integer>> getControllerId() {
        try {
            int controllerId = kraftMetadataService.getControllerId();
            return ResponseEntity.ok(Map.of("controllerId", controllerId));
        } catch (Exception e) {
            log.error("Failed to get controller ID", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
