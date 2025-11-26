# Chapter 12: Performance Tuning

Optimize Kafka for maximum throughput, minimum latency, or the right balance for your use case.

## Learning Objectives

- Tune producer for high throughput vs low latency
- Optimize consumer performance
- Understand broker configuration impact
- Run benchmarks and measure improvements

## Tuning Matrix

| Goal | batch.size | linger.ms | acks | compression |
|------|------------|-----------|------|-------------|
| **Max Throughput** | 64KB+ | 50-100ms | all | lz4/zstd |
| **Min Latency** | small | 0 | 1 | none/lz4 |
| **Balanced** | 16KB | 5-10ms | all | lz4 |

## Producer Tuning

### High Throughput

```yaml
spring:
  kafka:
    producer:
      batch-size: 65536      # 64KB
      buffer-memory: 67108864 # 64MB
      properties:
        linger.ms: 100
        compression.type: lz4
```

### Low Latency

```yaml
spring:
  kafka:
    producer:
      batch-size: 1
      properties:
        linger.ms: 0
        acks: 1
```

## Consumer Tuning

```yaml
spring:
  kafka:
    consumer:
      properties:
        fetch.min.bytes: 1024      # Wait for 1KB
        fetch.max.wait.ms: 500     # Or 500ms
        max.poll.records: 500
    listener:
      concurrency: 3               # Parallel consumers
```

## Benchmarking

```java
@Service
public class BenchmarkService {

    public BenchmarkResult runProducerBenchmark(int messages, int messageSize) {
        Instant start = Instant.now();
        String payload = "x".repeat(messageSize);

        for (int i = 0; i < messages; i++) {
            kafkaTemplate.send("benchmark", String.valueOf(i), payload);
        }
        kafkaTemplate.flush();

        Duration duration = Duration.between(start, Instant.now());
        double msgPerSec = messages / (duration.toMillis() / 1000.0);

        return new BenchmarkResult(messages, duration, msgPerSec);
    }
}
```

## Key Metrics to Monitor

- **Producer**: `record-send-rate`, `batch-size-avg`, `compression-rate`
- **Consumer**: `records-consumed-rate`, `fetch-latency-avg`, `records-lag`
- **Broker**: `MessagesInPerSec`, `BytesInPerSec`, `ReplicaLag`

## Interview Questions

1. **Q**: How do batch.size and linger.ms interact?
   **A**: Producer sends when either batch is full OR linger.ms expires. For throughput, increase both. For latency, set linger.ms=0.

## Next Chapter

Continue to [Chapter 13: Observability](../chapter-13-observability/README.md).
