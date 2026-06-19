package com.microgo.driver_location_generator.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverReachedDestinationEvent {
    private String driverId;
    private String rideId;
    private Instant occurredAt;
}
