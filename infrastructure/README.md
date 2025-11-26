# Infrastructure Setup

This directory contains Docker Compose configurations for running Kafka in KRaft mode (no ZooKeeper).

## Available Configurations

### 1. Single Broker (Development)

Best for: Learning, development, and running chapter examples.

```bash
docker-compose up -d
```

**Services:**
- Kafka broker: `localhost:9092`
- Kafka UI: `http://localhost:8080`

### 2. Multi-Broker Cluster

Best for: Testing replication, fault tolerance, and leader election.

```bash
docker-compose -f docker-compose-cluster.yml up -d
```

**Services:**
- Kafka broker 1: `localhost:9092`
- Kafka broker 2: `localhost:9095`
- Kafka broker 3: `localhost:9097`
- Kafka UI: `http://localhost:8080`

### 3. Full Stack (Production-like)

Best for: Chapters on observability, schema evolution, and production patterns.

```bash
docker-compose -f docker-compose-full.yml up -d
```

**Services:**
- Kafka brokers: `localhost:9092`, `localhost:9095`, `localhost:9097`
- Schema Registry: `http://localhost:8081`
- Kafka Connect: `http://localhost:8083`
- Kafka UI: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)
- Jaeger: `http://localhost:16686`

## Quick Reference

### Starting/Stopping

```bash
# Start
docker-compose up -d

# Stop (preserves data)
docker-compose down

# Stop and remove data
docker-compose down -v

# View logs
docker-compose logs -f kafka

# Check status
docker-compose ps
```

### Common Operations

```bash
# List topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Create a topic
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic my-topic --partitions 3 --replication-factor 1

# Describe a topic
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic my-topic

# Delete a topic
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --delete --topic my-topic

# Console producer
docker exec -it kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic my-topic

# Console consumer
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic my-topic --from-beginning

# Consumer groups
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Describe consumer group
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group my-group

# Reset consumer group offsets
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group my-group --topic my-topic --reset-offsets --to-earliest --execute
```

### Exploring Kafka Logs (Chapter 1)

```bash
# Access the Kafka data directory
docker exec -it kafka bash

# Navigate to log directory
cd /var/lib/kafka/data

# List topic partitions
ls -la

# View segment files for a topic
ls -la my-topic-0/

# Dump log contents
kafka-dump-log --files /var/lib/kafka/data/my-topic-0/00000000000000000000.log --print-data-log
```

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Network (kafka-network)              │
│                                                                 │
│  ┌─────────────────┐                                            │
│  │     Kafka       │──────────────────────┐                     │
│  │  (KRaft Mode)   │                      │                     │
│  │  Port: 9092     │                      ▼                     │
│  └─────────────────┘              ┌───────────────┐             │
│           │                       │   Kafka UI    │             │
│           │                       │  Port: 8080   │             │
│           ▼                       └───────────────┘             │
│  ┌─────────────────┐                                            │
│  │  Log Directory  │                                            │
│  │ /var/lib/kafka  │                                            │
│  │     /data       │                                            │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
            │
            │ Port 9092
            ▼
    ┌───────────────┐
    │ Host Machine  │
    │ (Your Apps)   │
    └───────────────┘
```

## Understanding KRaft Mode

Unlike traditional Kafka setups that require ZooKeeper, KRaft (Kafka Raft) mode:

1. **Eliminates ZooKeeper dependency** - Kafka manages its own metadata
2. **Uses Raft consensus** - For leader election and metadata replication
3. **Simpler operations** - One less system to manage and monitor
4. **Better scalability** - No ZooKeeper bottleneck for metadata operations

### Key Configuration Explained

```yaml
# Node identification
KAFKA_NODE_ID: 1                    # Unique ID for this broker

# KRaft roles
KAFKA_PROCESS_ROLES: 'broker,controller'  # This node acts as both

# Controller quorum (for multi-node clusters)
KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093'

# Cluster ID - must be same across all nodes
CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'  # Base64 encoded, 16 bytes
```

## Troubleshooting

### Kafka won't start

```bash
# Check logs
docker-compose logs kafka

# Common issues:
# 1. Port already in use - stop other Kafka instances
# 2. Insufficient memory - increase Docker memory allocation
# 3. Corrupt data - remove volumes: docker-compose down -v
```

### Cannot connect from application

```bash
# Verify Kafka is running
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Check listener configuration
docker exec kafka kafka-configs --bootstrap-server localhost:9092 \
  --entity-type brokers --entity-name 1 --describe

# Use correct bootstrap server:
# From host machine: localhost:9092
# From another container: kafka:9094
```

### Consumer lag issues

```bash
# Check consumer group lag
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group your-group-id

# List all consumer groups
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## Resource Requirements

| Configuration | CPU | Memory | Disk |
|--------------|-----|--------|------|
| Single Broker | 1 core | 2GB | 1GB |
| Cluster (3 brokers) | 2 cores | 6GB | 3GB |
| Full Stack | 4 cores | 8GB | 5GB |

Adjust Docker Desktop resources if needed.
