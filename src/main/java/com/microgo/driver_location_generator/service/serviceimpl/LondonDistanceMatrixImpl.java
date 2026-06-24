package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.businessrule.LondonDistanceBusinessRules;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.service.LondonDistanceMatrix;
import org.springframework.stereotype.Service;

@Service
public class LondonDistanceMatrixImpl implements LondonDistanceMatrix {

    @Override
    public double distanceMeters(GeoPoint origin, GeoPoint destination) {
        return LondonDistanceBusinessRules.distanceMeters(origin, destination);
    }

    @Override
    public GeoPoint moveToward(GeoPoint origin, GeoPoint destination, double stepMeters) {
        return LondonDistanceBusinessRules.moveToward(origin, destination, stepMeters);
    }
}
