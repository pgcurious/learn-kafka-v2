package com.kafka.learning.chapter02.service;

import com.kafka.learning.chapter02.model.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service for exploring KRaft mode metadata.
 *
 * This service demonstrates how to use the Kafka Admin Client to query
 * information about the KRaft controller quorum and cluster metadata.
 *
 * Key KRaft concepts:
 * - Controller Quorum: A set of nodes running Raft consensus for metadata
 * - Metadata Log: Internal topic (__cluster_metadata) storing all cluster state
 * - Voters: Controllers that participate in leader election
 * - Observers: Controllers that replicate metadata but don't vote
 */
@Service
public class KRaftMetadataService {

    private static final Logger log = LoggerFactory.getLogger(KRaftMetadataService.class);
    private static final int TIMEOUT_SECONDS = 30;

    private final AdminClient adminClient;

    public KRaftMetadataService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    /**
     * Get information about the controller quorum.
     *
     * This uses the DescribeQuorum API introduced with KRaft (KIP-595).
     * The quorum info includes:
     * - Current leader ID and epoch
     * - High watermark (committed offset)
     * - List of voters and their replication state
     * - List of observers (if any)
     *
     * @return QuorumInfoResponse containing quorum state
     */
    public QuorumInfoResponse getQuorumInfo() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Fetching controller quorum information...");

        DescribeQuorumResult result = adminClient.describeQuorum();
        QuorumInfo quorumInfo = result.quorumInfo().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        log.info("Quorum leader: {}, epoch: {}, high watermark: {}",
                quorumInfo.leaderId(), quorumInfo.leaderEpoch(), quorumInfo.highWatermark());

        List<ReplicaState> voters = quorumInfo.voters().stream()
                .map(this::toReplicaState)
                .collect(Collectors.toList());

        List<ReplicaState> observers = quorumInfo.observers().stream()
                .map(this::toReplicaState)
                .collect(Collectors.toList());

        return new QuorumInfoResponse(
                quorumInfo.leaderId(),
                quorumInfo.leaderEpoch(),
                quorumInfo.highWatermark(),
                voters,
                observers
        );
    }

    /**
     * Convert Kafka's ReplicaState to our model.
     */
    private ReplicaState toReplicaState(QuorumInfo.ReplicaState state) {
        return new ReplicaState(
                state.replicaId(),
                state.logEndOffset(),
                state.lastFetchTimestamp().orElse(-1L),
                state.lastCaughtUpTimestamp().orElse(-1L)
        );
    }

    /**
     * Get the current active controller information.
     *
     * In KRaft mode, one controller from the quorum is elected as the
     * active controller (leader). This controller handles all metadata
     * operations and coordinates with brokers.
     *
     * @return ControllerInfo about the active controller
     */
    public ControllerInfo getControllerInfo() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Fetching active controller information...");

        DescribeClusterResult clusterResult = adminClient.describeCluster();
        Node controller = clusterResult.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (controller == null) {
            throw new IllegalStateException("No controller available - cluster may be starting up");
        }

        log.info("Active controller: broker {} at {}:{}",
                controller.id(), controller.host(), controller.port());

        return new ControllerInfo(
                controller.id(),
                controller.host(),
                controller.port(),
                controller.rack()
        );
    }

    /**
     * Get the controller ID only.
     *
     * This is a lightweight call to just get the active controller's ID.
     *
     * @return The broker ID of the active controller
     */
    public int getControllerId() throws ExecutionException, InterruptedException, TimeoutException {
        return adminClient.describeCluster()
                .controller()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .id();
    }

    /**
     * Get metadata log information.
     *
     * The metadata log contains all cluster state in KRaft mode:
     * - TopicRecord: Topic creations
     * - PartitionRecord: Partition assignments
     * - BrokerRegistrationRecord: Broker lifecycle
     * - ConfigRecord: Topic/broker configurations
     * - And more...
     *
     * This method aggregates cluster state into a summary.
     *
     * @return MetadataLogInfo summarizing the metadata
     */
    public MetadataLogInfo getMetadataLogInfo() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Fetching metadata log information...");

        DescribeClusterResult clusterResult = adminClient.describeCluster();

        // Get cluster information
        String clusterId = clusterResult.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Node controller = clusterResult.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Collection<Node> nodes = clusterResult.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Get topic information (excluding internal topics for count)
        ListTopicsOptions options = new ListTopicsOptions().listInternal(false);
        Set<String> topics = adminClient.listTopics(options)
                .names()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Calculate total partitions
        int totalPartitions = 0;
        if (!topics.isEmpty()) {
            Map<String, TopicDescription> descriptions = adminClient
                    .describeTopics(topics)
                    .allTopicNames()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            totalPartitions = descriptions.values().stream()
                    .mapToInt(desc -> desc.partitions().size())
                    .sum();
        }

        // Build broker metadata list
        List<BrokerMetadata> brokers = nodes.stream()
                .map(node -> new BrokerMetadata(
                        node.id(),
                        node.host(),
                        node.port(),
                        node.rack(),
                        controller != null && node.id() == controller.id()
                ))
                .collect(Collectors.toList());

        log.info("Cluster: {}, brokers: {}, topics: {}, partitions: {}",
                clusterId, nodes.size(), topics.size(), totalPartitions);

        return new MetadataLogInfo(
                clusterId,
                controller != null ? controller.id() : -1,
                nodes.size(),
                topics.size(),
                totalPartitions,
                brokers
        );
    }

    /**
     * Get cluster health information.
     *
     * Checks various aspects of cluster health:
     * - Controller availability
     * - Quorum status
     * - Under-replicated partitions
     * - Offline partitions
     *
     * @return ClusterHealthInfo with health status and any issues
     */
    public ClusterHealthInfo getClusterHealth() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Checking cluster health...");

        List<String> issues = new ArrayList<>();
        boolean healthy = true;

        // Get controller info
        DescribeClusterResult clusterResult = adminClient.describeCluster();
        Node controller = clusterResult.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (controller == null) {
            issues.add("No active controller - cluster may be in election");
            healthy = false;
        }

        // Get quorum info
        QuorumInfo quorumInfo = adminClient.describeQuorum()
                .quorumInfo()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        int voterCount = quorumInfo.voters().size();
        int observerCount = quorumInfo.observers().size();

        // Check if quorum is healthy (majority of voters are caught up)
        long caughtUpVoters = quorumInfo.voters().stream()
                .filter(voter -> voter.lastCaughtUpTimestamp().isPresent())
                .count();

        if (caughtUpVoters < (voterCount / 2) + 1) {
            issues.add("Less than majority of voters are caught up");
            healthy = false;
        }

        // Check for lagging voters
        for (QuorumInfo.ReplicaState voter : quorumInfo.voters()) {
            long lag = quorumInfo.highWatermark() - voter.logEndOffset();
            if (lag > 100) { // Arbitrary threshold
                issues.add(String.format("Voter %d is lagging by %d records", voter.replicaId(), lag));
            }
        }

        // Count under-replicated and offline partitions
        int underReplicated = 0;
        int offline = 0;

        ListTopicsOptions options = new ListTopicsOptions().listInternal(false);
        Set<String> topics = adminClient.listTopics(options)
                .names()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!topics.isEmpty()) {
            Map<String, TopicDescription> descriptions = adminClient
                    .describeTopics(topics)
                    .allTopicNames()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            for (TopicDescription desc : descriptions.values()) {
                for (TopicPartitionInfo partition : desc.partitions()) {
                    if (partition.leader() == null) {
                        offline++;
                    } else if (partition.isr().size() < partition.replicas().size()) {
                        underReplicated++;
                    }
                }
            }
        }

        if (offline > 0) {
            issues.add(String.format("%d offline partition(s)", offline));
            healthy = false;
        }

        if (underReplicated > 0) {
            issues.add(String.format("%d under-replicated partition(s)", underReplicated));
        }

        log.info("Cluster health: {}, issues: {}", healthy ? "healthy" : "unhealthy", issues.size());

        return new ClusterHealthInfo(
                healthy,
                controller != null ? controller.id() : -1,
                voterCount + observerCount,
                voterCount,
                observerCount,
                underReplicated,
                offline,
                issues
        );
    }

    /**
     * Get feature flags supported by the cluster.
     *
     * KRaft introduces versioned features that can be upgraded independently.
     * This method shows which features are supported and their versions.
     *
     * @return Map of feature name to version range
     */
    public Map<String, String> getFeatureFlags() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Fetching feature flags...");

        DescribeFeaturesResult result = adminClient.describeFeatures();
        FeatureMetadata metadata = result.featureMetadata().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Map<String, String> features = new HashMap<>();

        // Get finalized features (currently active)
        metadata.finalizedFeatures().forEach((name, range) ->
                features.put(name, String.format("v%d (max: v%d)",
                        range.minVersionLevel(), range.maxVersionLevel()))
        );

        log.info("Found {} feature flags", features.size());
        return features;
    }

    /**
     * List all brokers in the cluster with their state.
     *
     * @return List of BrokerMetadata for all brokers
     */
    public List<BrokerMetadata> listBrokers() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Listing all brokers...");

        DescribeClusterResult clusterResult = adminClient.describeCluster();
        Node controller = clusterResult.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Collection<Node> nodes = clusterResult.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        return nodes.stream()
                .map(node -> new BrokerMetadata(
                        node.id(),
                        node.host(),
                        node.port(),
                        node.rack(),
                        controller != null && node.id() == controller.id()
                ))
                .sorted(Comparator.comparingInt(BrokerMetadata::brokerId))
                .collect(Collectors.toList());
    }
}
