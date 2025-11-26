package com.kafka.learning.chapter01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 1: Kafka Architecture Internals
 *
 * This application provides REST endpoints for exploring Kafka's internal
 * architecture, including topics, partitions, segments, and replication.
 *
 * Run with: mvn spring-boot:run
 * Then explore: http://localhost:8080/api/architecture
 */
@SpringBootApplication
public class Chapter01Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter01Application.class, args);
    }
}
