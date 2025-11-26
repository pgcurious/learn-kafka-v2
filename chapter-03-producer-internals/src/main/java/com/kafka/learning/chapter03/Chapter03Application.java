package com.kafka.learning.chapter03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 3: Producer Internals
 *
 * This application demonstrates Kafka producer configurations,
 * custom partitioners, and performance optimization techniques.
 *
 * Topics covered:
 * - Batching and buffering
 * - Partitioning strategies
 * - Idempotent producers
 * - Performance tuning
 */
@SpringBootApplication
public class Chapter03Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter03Application.class, args);
    }
}
