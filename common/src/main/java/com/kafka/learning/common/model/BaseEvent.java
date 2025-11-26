package com.kafka.learning.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 *
 * Every event should extend this class to ensure consistent structure
 * across all Kafka messages. This enables:
 * - Event correlation through eventId
 * - Time-based ordering and analysis
 * - Event versioning for schema evolution
 * - Tracing through correlationId
 */
public abstract class BaseEvent {

    private final String eventId;
    private final String eventType;
    private final Instant timestamp;
    private final int version;
    private String correlationId;
    private String causationId;

    protected BaseEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.version = 1;
    }

    protected BaseEvent(String eventType, int version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.version = version;
    }

    // For deserialization
    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.timestamp = Instant.now();
        this.version = 1;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getVersion() {
        return version;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, timestamp=%s, version=%d]",
                eventType, eventId, timestamp, version);
    }
}
