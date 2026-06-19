package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MovementUpdate {
    DriverGeoState newState;
    String rideId;
    boolean reachedPickup;
    boolean reachedDestination;
    boolean becameAvailable;
    boolean zoneChanged;
}
