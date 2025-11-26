package com.kafka.learning.chapter01.explorer;

import com.kafka.learning.chapter01.model.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service for exploring Kafka cluster internals.
 *
 * This class demonstrates how to use the Kafka Admin Client to inspect
 * cluster metadata, topic configurations, and partition information.
 *
 * Learning objectives:
 * - Understand cluster topology (brokers, controller)
 * - Explore topic and partition metadata
 * - Analyze replica distribution and ISR
 * - View log directory information
 */
@Service
public class KafkaClusterExplorer {

    private static final Logger log = LoggerFactory.getLogger(KafkaClusterExplorer.class);
    private static final int TIMEOUT_SECONDS = 30;

    private final AdminClient adminClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Constructor injection - Spring Boot best practice
    public KafkaClusterExplorer(AdminClient adminClient,
                                 KafkaTemplate<String, String> kafkaTemplate) {
        this.adminClient = adminClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Retrieves comprehensive cluster information.
     *
     * This method shows:
     * - Cluster ID (unique identifier)
     * - Controller broker (handles admin operations)
     * - All broker nodes in the cluster
     */
    public ClusterInfo getClusterInfo() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Fetching cluster information...");

        DescribeClusterResult clusterResult = adminClient.describeCluster();

        // All these calls are async - we block for simplicity
        String clusterId = clusterResult.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Node controller = clusterResult.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Collection<Node> nodes = clusterResult.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        log.info("Cluster ID: {}, Controller: {}, Nodes: {}",
                clusterId, controller.id(), nodes.size());

        List<BrokerInfo> brokers = nodes.stream()
                .map(node -> new BrokerInfo(
                        node.id(),
                        node.host(),
                        node.port(),
                        node.rack(),
                        node.id() == controller.id()
                ))
                .toList();

        return new ClusterInfo(clusterId, controller.id(), brokers);
    }

    /**
     * Lists all topics in the cluster.
     *
     * The listTopics() method returns both:
     * - User-created topics
     * - Internal topics (prefixed with __)
     *
     * Internal topics include:
     * - __consumer_offsets: Stores consumer group offsets
     * - __transaction_state: Stores transaction metadata
     */
    public List<String> listTopics(boolean includeInternal) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Listing topics (includeInternal={})", includeInternal);

        ListTopicsOptions options = new ListTopicsOptions()
                .listInternal(includeInternal);

        Set<String> topics = adminClient.listTopics(options)
                .names()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        log.info("Found {} topics", topics.size());
        return new ArrayList<>(topics);
    }

    /**
     * Retrieves detailed information about a specific topic.
     *
     * This includes:
     * - Partition count
     * - Replication factor
     * - Leader and replica assignments
     * - ISR (In-Sync Replicas) for each partition
     */
    public TopicDetails getTopicDetails(String topicName) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Fetching details for topic: {}", topicName);

        // Describe the topic
        Map<String, TopicDescription> descriptions = adminClient
                .describeTopics(Collections.singletonList(topicName))
                .allTopicNames()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        TopicDescription description = descriptions.get(topicName);
        if (description == null) {
            throw new IllegalArgumentException("Topic not found: " + topicName);
        }

        // Get partition details
        List<PartitionDetails> partitions = description.partitions().stream()
                .map(this::toPartitionDetails)
                .toList();

        // Get topic configuration
        Map<String, String> configs = getTopicConfigs(topicName);

        // Calculate replication factor from first partition
        int replicationFactor = description.partitions().isEmpty()
                ? 0
                : description.partitions().get(0).replicas().size();

        return new TopicDetails(
                topicName,
                description.partitions().size(),
                replicationFactor,
                description.isInternal(),
                partitions,
                configs
        );
    }

    /**
     * Converts Kafka's TopicPartitionInfo to our PartitionDetails model.
     */
    private PartitionDetails toPartitionDetails(TopicPartitionInfo partitionInfo) {
        return new PartitionDetails(
                partitionInfo.partition(),
                partitionInfo.leader() != null ? partitionInfo.leader().id() : -1,
                partitionInfo.replicas().stream().map(Node::id).toList(),
                partitionInfo.isr().stream().map(Node::id).toList()
        );
    }

    /**
     * Retrieves configuration values for a topic.
     *
     * Key configurations to understand:
     * - segment.bytes: Size at which new segments are created
     * - retention.ms: How long to keep messages
     * - cleanup.policy: delete or compact
     * - min.insync.replicas: Minimum ISR for acks=all
     */
    public Map<String, String> getTopicConfigs(String topicName) throws ExecutionException, InterruptedException, TimeoutException {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

        Map<ConfigResource, Config> configs = adminClient
                .describeConfigs(Collections.singletonList(resource))
                .all()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Config topicConfig = configs.get(resource);

        return topicConfig.entries().stream()
                .collect(Collectors.toMap(
                        ConfigEntry::name,
                        ConfigEntry::value
                ));
    }

    /**
     * Creates a topic for exploration.
     *
     * This method demonstrates topic creation with custom configurations.
     * Understanding these settings is crucial for production deployments.
     */
    public void createExplorationTopic(String topicName, int partitions, short replicationFactor)
            throws ExecutionException, InterruptedException, TimeoutException {

        log.info("Creating topic: {} with {} partitions and RF={}",
                topicName, partitions, replicationFactor);

        // Topic configuration
        Map<String, String> configs = new HashMap<>();
        // Small segment size for exploration - see segment files quickly
        configs.put("segment.bytes", "1048576"); // 1MB
        // Short retention for exploration
        configs.put("retention.ms", "3600000"); // 1 hour
        // Minimum ISR
        configs.put("min.insync.replicas", "1");

        NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor)
                .configs(configs);

        try {
            adminClient.createTopics(Collections.singletonList(newTopic))
                    .all()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Topic created successfully: {}", topicName);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                log.info("Topic already exists: {}", topicName);
            } else {
                throw e;
            }
        }
    }

    /**
     * Produces messages to a topic for exploration.
     *
     * This helps generate data to observe:
     * - Segment file creation
     * - Offset assignment
     * - Partition distribution
     */
    public void produceTestMessages(String topicName, int count) {
        log.info("Producing {} messages to topic: {}", count, topicName);

        for (int i = 0; i < count; i++) {
            String key = "key-" + (i % 10); // 10 unique keys for partition distribution
            String value = "message-" + i + "-" + System.currentTimeMillis();

            kafkaTemplate.send(topicName, key, value);

            if ((i + 1) % 100 == 0) {
                log.info("Produced {} messages", i + 1);
            }
        }

        kafkaTemplate.flush();
        log.info("Finished producing {} messages", count);
    }

    /**
     * Gets offset information for each partition.
     *
     * This shows:
     * - Beginning offset (earliest available)
     * - End offset (next offset to be written)
     * - Message count (end - beginning)
     */
    public List<PartitionOffsetInfo> getPartitionOffsets(String topicName)
            throws ExecutionException, InterruptedException, TimeoutException {

        // First, get partition info
        TopicDetails details = getTopicDetails(topicName);

        List<TopicPartition> partitions = details.partitions().stream()
                .map(p -> new TopicPartition(topicName, p.partition()))
                .toList();

        // Get beginning offsets
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> beginningOffsets =
                adminClient.listOffsets(
                        partitions.stream().collect(Collectors.toMap(
                                tp -> tp,
                                tp -> OffsetSpec.earliest()
                        ))
                ).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Get end offsets
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                adminClient.listOffsets(
                        partitions.stream().collect(Collectors.toMap(
                                tp -> tp,
                                tp -> OffsetSpec.latest()
                        ))
                ).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        return partitions.stream()
                .map(tp -> {
                    long beginning = beginningOffsets.get(tp).offset();
                    long end = endOffsets.get(tp).offset();
                    return new PartitionOffsetInfo(
                            tp.partition(),
                            beginning,
                            end,
                            end - beginning
                    );
                })
                .toList();
    }

    /**
     * Gets log directory information for brokers.
     *
     * This shows how data is distributed across broker disks,
     * useful for understanding storage and capacity planning.
     */
    public Map<Integer, List<LogDirInfo>> getLogDirInfo()
            throws ExecutionException, InterruptedException, TimeoutException {

        // Get all broker IDs
        Collection<Node> nodes = adminClient.describeCluster()
                .nodes()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        List<Integer> brokerIds = nodes.stream()
                .map(Node::id)
                .toList();

        // Describe log directories
        Map<Integer, Map<String, LogDirDescription>> logDirs = adminClient
                .describeLogDirs(brokerIds)
                .allDescriptions()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Transform to our model
        Map<Integer, List<LogDirInfo>> result = new HashMap<>();

        for (Map.Entry<Integer, Map<String, LogDirDescription>> brokerEntry : logDirs.entrySet()) {
            List<LogDirInfo> dirs = new ArrayList<>();

            for (Map.Entry<String, LogDirDescription> dirEntry : brokerEntry.getValue().entrySet()) {
                LogDirDescription desc = dirEntry.getValue();

                long totalSize = desc.replicaInfos().values().stream()
                        .mapToLong(ReplicaInfo::size)
                        .sum();

                dirs.add(new LogDirInfo(
                        dirEntry.getKey(),
                        desc.replicaInfos().size(),
                        totalSize,
                        desc.error() != null ? desc.error().getMessage() : null
                ));
            }

            result.put(brokerEntry.getKey(), dirs);
        }

        return result;
    }
}
