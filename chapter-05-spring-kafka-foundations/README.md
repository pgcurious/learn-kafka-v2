# Chapter 5: Spring Kafka Foundations

This chapter covers the core Spring Kafka components: `KafkaTemplate`, `@KafkaListener`, serialization, and configuration patterns.

## Learning Objectives

By the end of this chapter, you will:
- Master `KafkaTemplate` for producing messages
- Understand `@KafkaListener` internals and configuration
- Implement various serialization strategies (JSON, custom)
- Apply Spring Boot configuration best practices

## Spring Kafka Architecture

```mermaid
graph TB
    subgraph "Producer Side"
        PS[ProducerService]
        KT[KafkaTemplate]
        PF[ProducerFactory]
        KS[Key/Value Serializers]
    end

    subgraph "Consumer Side"
        KL[@KafkaListener]
        MLC[MessageListenerContainer]
        CF[ConsumerFactory]
        KD[Key/Value Deserializers]
    end

    subgraph "Kafka Cluster"
        K[Kafka Brokers]
    end

    PS --> KT
    KT --> PF
    PF --> KS
    KS --> K

    K --> KD
    KD --> CF
    CF --> MLC
    MLC --> KL
```

## KafkaTemplate Deep Dive

### Basic Usage

```java
@Service
public class OrderProducerService {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    // Constructor injection (preferred over @Autowired)
    public OrderProducerService(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Order>> sendOrder(Order order) {
        // send() returns a CompletableFuture (non-blocking)
        return kafkaTemplate.send("orders", order.orderId(), order);
    }
}
```

### Send Methods

| Method | Description |
|--------|-------------|
| `send(topic, value)` | Send with null key |
| `send(topic, key, value)` | Send with key (partition by key) |
| `send(topic, partition, key, value)` | Explicit partition |
| `send(ProducerRecord)` | Full control with headers |
| `sendDefault(key, value)` | Use default topic |

### Async vs Sync Sending

```java
// Async (recommended for throughput)
kafkaTemplate.send(topic, key, value)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            handleError(ex);
        }
    });

// Sync (when you need confirmation)
try {
    SendResult<String, Order> result =
        kafkaTemplate.send(topic, key, value).get(10, TimeUnit.SECONDS);
    log.info("Sent to partition {}", result.getRecordMetadata().partition());
} catch (ExecutionException e) {
    handleError(e.getCause());
}
```

## @KafkaListener Internals

### Basic Listener

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void processOrder(Order order) {
    log.info("Processing order: {}", order.orderId());
    // Process the order...
}
```

### Accessing Message Metadata

```java
@KafkaListener(topics = "orders", groupId = "order-processor")
public void processOrder(
        Order order,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack) {

    log.info("Received: partition={}, offset={}, key={}", partition, offset, key);
    processOrder(order);
    ack.acknowledge();
}
```

### Batch Listener

```java
@KafkaListener(topics = "orders", groupId = "batch-processor",
               containerFactory = "batchFactory")
public void processBatch(List<Order> orders) {
    log.info("Processing batch of {} orders", orders.size());
    orders.forEach(this::processOrder);
}
```

### Listener Container Factory

```java
@Configuration
public class KafkaListenerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order>
            kafkaListenerContainerFactory(ConsumerFactory<String, Order> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Order> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);  // 3 consumer threads

        // Manual acknowledgment
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }
}
```

## Serialization Strategies

### JSON Serialization

```java
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Type mapping for deserialization
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafka.learning.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Order.class.getName());

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
```

### Type-Safe Producer

```java
@Configuration
public class TypeSafeKafkaConfig {

    @Bean
    public KafkaTemplate<String, Order> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Payment> paymentKafkaTemplate() {
        return new KafkaTemplate<>(paymentProducerFactory());
    }

    // Separate factories for type safety
    private ProducerFactory<String, Order> orderProducerFactory() {
        JsonSerializer<Order> serializer = new JsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(
            producerProps(),
            new StringSerializer(),
            serializer
        );
    }
}
```

## Configuration Properties

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5

    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "com.kafka.learning.*"

    listener:
      ack-mode: manual
      concurrency: 3
```

### @ConfigurationProperties

```java
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
    String ordersTopic,
    String paymentsTopic,
    int consumerConcurrency,
    Duration pollTimeout
) {}
```

## Hands-On Lab

### Step 1: Start Infrastructure

```bash
cd ../infrastructure
docker-compose up -d
```

### Step 2: Run the Application

```bash
cd ../chapter-05-spring-kafka-foundations
mvn spring-boot:run
```

### Step 3: Test Producer

```bash
# Send an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "cust-1", "items": [{"productId": "prod-1", "quantity": 2}]}'

# Send multiple orders
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{\"customerId\": \"cust-$i\", \"items\": [{\"productId\": \"prod-1\", \"quantity\": $i}]}"
done
```

### Step 4: View Consumer Stats

```bash
curl http://localhost:8080/api/stats
```

## Best Practices

### 1. Use Constructor Injection

```java
// Good
public OrderService(KafkaTemplate<String, Order> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
}

// Avoid
@Autowired
private KafkaTemplate<String, Order> kafkaTemplate;
```

### 2. Configure Appropriate Serializers

```java
// Use type-specific templates
KafkaTemplate<String, Order> orderTemplate;
KafkaTemplate<String, Payment> paymentTemplate;
```

### 3. Handle Send Failures

```java
kafkaTemplate.send(topic, order)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Send failed for order {}", order.orderId(), ex);
            // Implement retry or dead-letter logic
        }
    });
```

### 4. Use Manual Acknowledgment for Reliability

```java
@KafkaListener(topics = "orders")
public void process(Order order, Acknowledgment ack) {
    try {
        processOrder(order);
        ack.acknowledge();  // Only ack after successful processing
    } catch (Exception e) {
        // Don't ack - message will be redelivered
        throw e;
    }
}
```

## Interview Questions

### Beginner
1. **Q**: What is `KafkaTemplate` and how does it differ from `KafkaProducer`?
   **A**: `KafkaTemplate` is Spring's high-level abstraction over `KafkaProducer`. It provides auto-configuration, transaction support, Spring-style callbacks, and integration with Spring's dependency injection. `KafkaProducer` is the lower-level Kafka client.

### Intermediate
2. **Q**: How does `@KafkaListener` work internally?
   **A**: Spring creates a `MessageListenerContainer` for each `@KafkaListener`. The container manages consumer threads, polls Kafka, deserializes messages, and invokes the annotated method. Multiple containers can run concurrently based on the `concurrency` setting.

### Advanced
3. **Q**: How would you implement type-safe Kafka messaging with multiple message types?
   **A**: Create separate `KafkaTemplate` beans for each type with appropriate serializers. Use type headers (`__TypeId__`) for deserialization. Consider using a message wrapper or envelope pattern for routing.

## References

- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Spring Boot Kafka Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.kafka)

## Next Chapter

Continue to [Chapter 6: Error Handling Patterns](../chapter-06-error-handling/README.md) to learn how to build resilient consumers.
