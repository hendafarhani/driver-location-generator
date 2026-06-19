package com.microgo.driver_location_generator.kafka.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaListenerConfiguration {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DriverGeneratedEvent> driverGeneratedListenerFactory(
            ConsumerFactory<String, DriverGeneratedEvent> driverGeneratedConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DriverGeneratedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(driverGeneratedConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DriverGeneratedEvent> driverGeneratedConsumerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return consumerFactory(bootstrapServers, DriverGeneratedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RepositionDriverCommand> repositionDriverCommandListenerFactory(
            ConsumerFactory<String, RepositionDriverCommand> repositionDriverCommandConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, RepositionDriverCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(repositionDriverCommandConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, RepositionDriverCommand> repositionDriverCommandConsumerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return consumerFactory(bootstrapServers, RepositionDriverCommand.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MoveToPickupCommand> moveToPickupCommandListenerFactory(
            ConsumerFactory<String, MoveToPickupCommand> moveToPickupCommandConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MoveToPickupCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(moveToPickupCommandConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, MoveToPickupCommand> moveToPickupCommandConsumerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return consumerFactory(bootstrapServers, MoveToPickupCommand.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StartTripCommand> startTripCommandListenerFactory(
            ConsumerFactory<String, StartTripCommand> startTripCommandConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, StartTripCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(startTripCommandConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, StartTripCommand> startTripCommandConsumerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return consumerFactory(bootstrapServers, StartTripCommand.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StopDriverCommand> stopDriverCommandListenerFactory(
            ConsumerFactory<String, StopDriverCommand> stopDriverCommandConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, StopDriverCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stopDriverCommandConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, StopDriverCommand> stopDriverCommandConsumerFactory(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return consumerFactory(bootstrapServers, StopDriverCommand.class);
    }

    private <T> ConsumerFactory<String, T> consumerFactory(String bootstrapServers, Class<T> payloadType) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        ErrorHandlingDeserializer<T> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer(payloadType));
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), errorHandlingDeserializer);
    }

    private <T> Deserializer<T> jsonDeserializer(Class<T> payloadType) {
        ObjectMapper objectMapper = new ObjectMapper();
        return (topic, data) -> {
            if (data == null || data.length == 0) {
                return null;
            }
            try {
                return objectMapper.readValue(data, payloadType);
            } catch (IOException exception) {
                throw new SerializationException("Failed to deserialize Kafka payload for topic " + topic, exception);
            }
        };
    }
}
