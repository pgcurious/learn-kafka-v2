package com.kafka.learning.chapter01.model;

/**
 * Information about a Kafka broker.
 *
 * @param id          Unique broker ID
 * @param host        Hostname or IP address
 * @param port        Port number
 * @param rack        Rack identifier (for rack-aware replication)
 * @param isController Whether this broker is the current controller
 */
public record BrokerInfo(
        int id,
        String host,
        int port,
        String rack,
        boolean isController
) {
}
