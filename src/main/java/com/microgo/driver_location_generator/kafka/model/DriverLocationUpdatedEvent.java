package com.microgo.driver_location_generator.kafka.model;

import com.microgo.driver_location_generator.domain.DriverStatus;
import com.microgo.driver_location_generator.domain.LondonScenario;
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
public class DriverLocationUpdatedEvent {
    private String driverId;
    private String driverIdentifier;
    private String providerIdentifier;
    private String driverDisplayId;
    private LondonScenario scenario;
    private DriverStatus status;
    private LondonZone zone;
    private double latitude;
    private double longitude;
    private boolean available;
    private long tickSequence;
    private Instant occurredAt;
}
