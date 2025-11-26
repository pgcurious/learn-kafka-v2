package com.kafka.learning.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Centralized Jackson ObjectMapper configuration.
 *
 * This ensures consistent JSON serialization/deserialization across all modules.
 * Key features:
 * - Java 8 date/time support (Instant, LocalDateTime, etc.)
 * - Graceful handling of unknown properties (for schema evolution)
 * - ISO-8601 date format instead of timestamps
 */
public final class JacksonConfig {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();

        // Register Java 8 date/time module
        OBJECT_MAPPER.registerModule(new JavaTimeModule());

        // Use ISO-8601 format for dates instead of timestamps
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't fail on unknown properties - enables forward compatibility
        // New fields added to messages won't break old consumers
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Allow empty beans to be serialized
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    private JacksonConfig() {
        // Utility class - prevent instantiation
    }

    /**
     * Returns a pre-configured ObjectMapper instance.
     * This instance is thread-safe and can be shared.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Creates a new ObjectMapper with the same configuration.
     * Use this when you need to customize the mapper without affecting the shared instance.
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
