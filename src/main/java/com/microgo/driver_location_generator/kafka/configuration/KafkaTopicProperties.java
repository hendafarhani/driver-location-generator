package com.microgo.driver_location_generator.kafka.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {
    private String driverGenerated;
    private String driverLocationUpdated;
    private String driverEnteredZone;
    private String driverReachedPickup;
    private String driverReachedDestination;
    private String driverBecameAvailable;
    private String repositionDriverCommand;
    private String moveToPickupCommand;
    private String startTripCommand;
    private String stopDriverCommand;
}
