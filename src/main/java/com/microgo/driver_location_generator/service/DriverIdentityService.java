package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.entity.DriverProfileEntity;

public interface DriverIdentityService {

    DriverProfileEntity ensureProfile(String driverIdentifier, String requestedDisplayId);
}
