package com.kafka.learning.chapter02.model;

/**
 * Metadata about a broker in the cluster.
 *
 * @param brokerId Unique identifier for the broker
 * @param host Hostname of the broker
 * @param port Port the broker is listening on
 * @param rack Rack identifier (for rack-aware replication)
 * @param isController Whether this broker is currently the active controller
 */
public record BrokerMetadata(
        int brokerId,
        String host,
        int port,
        String rack,
        boolean isController
) {
}
