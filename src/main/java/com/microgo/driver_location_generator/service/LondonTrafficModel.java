package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.domain.DriverGeoState;

public interface LondonTrafficModel {

    double stepMeters(DriverGeoState state);
}
