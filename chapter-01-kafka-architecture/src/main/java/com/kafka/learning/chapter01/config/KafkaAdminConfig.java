package com.kafka.learning.chapter01.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for Chapter 1.
 *
 * This chapter uses the Admin Client extensively to explore cluster metadata.
 * The Admin Client provides methods to:
 * - Describe cluster, topics, and configurations
 * - Create, delete, and alter topics
 * - Manage consumer groups
 * - View log directories and replica information
 */
@Configuration
public class KafkaAdminConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Creates an AdminClient bean for cluster exploration.
     *
     * The AdminClient is the entry point for administrative operations.
     * It's thread-safe and should be reused (not created per request).
     */
    @Bean
    public AdminClient adminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Request timeout - how long to wait for a response
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        // Default API timeout for blocking operations
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);

        return AdminClient.create(configs);
    }

    /**
     * Producer factory for sending test messages.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // For exploration, we want to see effects quickly
        configs.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, 1);

        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
