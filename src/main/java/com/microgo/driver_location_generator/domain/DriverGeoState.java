package com.microgo.driver_location_generator.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DriverGeoState {
    private String driverId;
    private String driverDisplayId;
    private String activeRideId;
    private DriverStatus status;
    private LondonScenario scenario;
    private GeoPoint currentPosition;
    private GeoPoint targetPosition;
    private GeoPoint pickupPosition;
    private GeoPoint dropoffPosition;
    private LondonZone currentZone;
    private boolean available;
    private double plannedStepMeters;
    private long tickSequence;
    private Instant updatedAt;

    public boolean hasTargetPosition() {
        return targetPosition != null;
    }
}
