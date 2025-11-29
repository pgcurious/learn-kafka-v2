package com.kafka.learning.chapter02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 2: KRaft Mode Deep Dive
 *
 * This application provides REST endpoints for exploring KRaft mode,
 * the Raft-based consensus protocol that replaced ZooKeeper in Kafka.
 *
 * Key concepts covered:
 * - Controller quorum and leader election
 * - Metadata management without ZooKeeper
 * - KRaft configuration and troubleshooting
 *
 * Run with: mvn spring-boot:run
 * Then explore: http://localhost:8080/api/kraft
 */
@SpringBootApplication
public class Chapter02Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter02Application.class, args);
    }
}
