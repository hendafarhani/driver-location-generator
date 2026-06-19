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
public class DriverBecameAvailableEvent {
    private String driverId;
    private Instant occurredAt;
}
