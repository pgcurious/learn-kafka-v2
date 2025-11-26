package com.kafka.learning.chapter04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 4: Consumer Internals
 *
 * This application demonstrates Kafka consumer configurations,
 * offset management strategies, and rebalancing protocols.
 *
 * Topics covered:
 * - Consumer groups and coordination
 * - Partition assignment strategies
 * - Manual vs auto offset commit
 * - Rebalance listeners
 */
@SpringBootApplication
public class Chapter04Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter04Application.class, args);
    }
}
