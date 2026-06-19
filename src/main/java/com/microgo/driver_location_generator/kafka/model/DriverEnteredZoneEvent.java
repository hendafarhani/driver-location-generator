package com.microgo.driver_location_generator.kafka.model;

import com.microgo.driver_location_generator.domain.LondonZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverEnteredZoneEvent {
    private String driverId;
    private LondonZone zone;
    private Instant occurredAt;
}
