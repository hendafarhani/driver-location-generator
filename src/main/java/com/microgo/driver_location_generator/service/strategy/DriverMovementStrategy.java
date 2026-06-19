package com.microgo.driver_location_generator.service.strategy;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.service.MovementUpdate;

public interface DriverMovementStrategy {
    boolean supports(DriverGeoState state);
    MovementUpdate move(DriverGeoState state);
}
