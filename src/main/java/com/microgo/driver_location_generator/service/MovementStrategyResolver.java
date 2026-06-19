package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.service.strategy.DriverMovementStrategy;

public interface MovementStrategyResolver {

    DriverMovementStrategy resolve(DriverGeoState state);
}
