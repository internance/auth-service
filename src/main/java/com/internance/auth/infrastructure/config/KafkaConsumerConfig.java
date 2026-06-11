package com.internance.auth.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 * Consumer-side counterpart to config-service's JSON producer setup.
 *
 * <p>config-service publishes the {@code EventEnvelope} as plain JSON with type-id
 * headers disabled, so we cannot recover the payload type from the record. Instead we
 * register a {@link ByteArrayJsonMessageConverter} (built from the shared
 * {@link ObjectMapper}, which already handles the envelope's {@code Instant occurredAt}
 * via common-lib's {@code JacksonConfig}). Spring Boot wires the single
 * {@link RecordMessageConverter} bean into the auto-configured listener container
 * factory, and it deserializes each record against the generic declared on the
 * listener method — {@code EventEnvelope<ConfigChangedEvent>}.
 *
 * <p>Pairs with {@code spring.kafka.consumer.value-deserializer=ByteArrayDeserializer}
 * so the converter receives the raw {@code byte[]} value.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public RecordMessageConverter configEventMessageConverter(ObjectMapper objectMapper) {
        return new ByteArrayJsonMessageConverter(objectMapper);
    }
}
