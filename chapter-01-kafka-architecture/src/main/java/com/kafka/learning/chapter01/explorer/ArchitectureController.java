package com.kafka.learning.chapter01.explorer;

import com.kafka.learning.chapter01.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for exploring Kafka architecture.
 *
 * Endpoints:
 * - GET  /api/architecture/cluster-info    - Cluster overview
 * - GET  /api/architecture/topics          - List all topics
 * - GET  /api/architecture/topics/{name}   - Topic details
 * - POST /api/architecture/setup           - Create exploration topic
 * - POST /api/architecture/produce         - Produce test messages
 * - GET  /api/architecture/offsets/{topic} - Partition offsets
 * - GET  /api/architecture/log-dirs        - Log directory info
 */
@RestController
@RequestMapping("/api/architecture")
public class ArchitectureController {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureController.class);
    private static final String EXPLORATION_TOPIC = "architecture.demo";

    private final KafkaClusterExplorer clusterExplorer;

    public ArchitectureController(KafkaClusterExplorer clusterExplorer) {
        this.clusterExplorer = clusterExplorer;
    }

    /**
     * Get cluster information including brokers and controller.
     */
    @GetMapping("/cluster-info")
    public ResponseEntity<ClusterInfo> getClusterInfo() {
        try {
            ClusterInfo info = clusterExplorer.getClusterInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get cluster info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * List all topics in the cluster.
     *
     * @param includeInternal Whether to include internal topics (__consumer_offsets, etc.)
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> listTopics(
            @RequestParam(defaultValue = "false") boolean includeInternal) {
        try {
            List<String> topics = clusterExplorer.listTopics(includeInternal);
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            log.error("Failed to list topics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get detailed information about a specific topic.
     */
    @GetMapping("/topics/{topicName}")
    public ResponseEntity<TopicDetails> getTopicDetails(@PathVariable String topicName) {
        try {
            TopicDetails details = clusterExplorer.getTopicDetails(topicName);
            return ResponseEntity.ok(details);
        } catch (IllegalArgumentException e) {
            log.warn("Topic not found: {}", topicName);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get topic details: {}", topicName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Setup the exploration environment.
     *
     * Creates the architecture.demo topic with small segments
     * for easy exploration of log files.
     */
    @PostMapping("/setup")
    public ResponseEntity<String> setup() {
        try {
            clusterExplorer.createExplorationTopic(EXPLORATION_TOPIC, 3, (short) 1);
            return ResponseEntity.ok("Setup complete. Topic created: " + EXPLORATION_TOPIC);
        } catch (Exception e) {
            log.error("Setup failed", e);
            return ResponseEntity.internalServerError()
                    .body("Setup failed: " + e.getMessage());
        }
    }

    /**
     * Produce test messages to the exploration topic.
     *
     * @param count Number of messages to produce (default 100)
     */
    @PostMapping("/produce")
    public ResponseEntity<String> produceMessages(
            @RequestParam(defaultValue = "100") int count) {
        try {
            clusterExplorer.produceTestMessages(EXPLORATION_TOPIC, count);
            return ResponseEntity.ok("Produced " + count + " messages to " + EXPLORATION_TOPIC);
        } catch (Exception e) {
            log.error("Failed to produce messages", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to produce: " + e.getMessage());
        }
    }

    /**
     * Get offset information for each partition of a topic.
     */
    @GetMapping("/offsets/{topicName}")
    public ResponseEntity<List<PartitionOffsetInfo>> getOffsets(@PathVariable String topicName) {
        try {
            List<PartitionOffsetInfo> offsets = clusterExplorer.getPartitionOffsets(topicName);
            return ResponseEntity.ok(offsets);
        } catch (Exception e) {
            log.error("Failed to get offsets for topic: {}", topicName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get log directory information for all brokers.
     *
     * Shows where data is stored and how much space is used.
     */
    @GetMapping("/log-dirs")
    public ResponseEntity<Map<Integer, List<LogDirInfo>>> getLogDirs() {
        try {
            Map<Integer, List<LogDirInfo>> logDirs = clusterExplorer.getLogDirInfo();
            return ResponseEntity.ok(logDirs);
        } catch (Exception e) {
            log.error("Failed to get log directory info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get topic configuration.
     */
    @GetMapping("/topics/{topicName}/config")
    public ResponseEntity<Map<String, String>> getTopicConfig(@PathVariable String topicName) {
        try {
            Map<String, String> config = clusterExplorer.getTopicConfigs(topicName);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Failed to get config for topic: {}", topicName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
