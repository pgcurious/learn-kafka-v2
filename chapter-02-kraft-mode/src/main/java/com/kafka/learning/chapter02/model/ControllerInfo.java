package com.kafka.learning.chapter02.model;

/**
 * Information about the active controller in the cluster.
 *
 * In KRaft mode, one controller is elected as the active controller
 * (leader) and handles all metadata operations.
 *
 * @param controllerId The broker ID of the active controller
 * @param host The hostname of the controller
 * @param port The port the controller is listening on
 * @param rack The rack identifier (if configured)
 */
public record ControllerInfo(
        int controllerId,
        String host,
        int port,
        String rack
) {
}
