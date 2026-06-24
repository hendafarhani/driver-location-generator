package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.businessrule.LondonTrafficBusinessRules;
import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.service.LondonTrafficModel;
import org.springframework.stereotype.Service;

@Service
public class LondonTrafficModelImpl implements LondonTrafficModel {

    private final DriverLocationGeneratorProperties properties;

    public LondonTrafficModelImpl(DriverLocationGeneratorProperties properties) {
        this.properties = properties;
    }

    @Override
    public double stepMeters(DriverGeoState state) {
        return LondonTrafficBusinessRules.stepMeters(state, properties);
    }
}
