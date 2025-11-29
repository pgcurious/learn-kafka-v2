package com.kafka.learning.chapter02.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Admin Client configuration for Chapter 2.
 *
 * This chapter focuses on KRaft mode exploration, using the Admin Client
 * to query controller quorum information and metadata.
 *
 * Key Admin Client operations for KRaft:
 * - describeQuorum(): Get information about the controller quorum
 * - describeCluster(): Get cluster metadata including controller
 * - describeMetadataQuorum(): Detailed quorum state (Kafka 3.3+)
 */
@Configuration
public class KafkaAdminConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Creates an AdminClient bean for KRaft exploration.
     *
     * The AdminClient provides access to KRaft-specific APIs like
     * describeQuorum() which returns information about the controller
     * quorum including leader, voters, and replication lag.
     */
    @Bean
    public AdminClient adminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Request timeout for admin operations
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        // Default API timeout for blocking operations
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);

        // Retry configuration for transient failures
        configs.put(AdminClientConfig.RETRIES_CONFIG, 3);
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        return AdminClient.create(configs);
    }
}
