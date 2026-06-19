package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.domain.GeoPoint;

public interface LondonDistanceMatrix {

    double distanceMeters(GeoPoint origin, GeoPoint destination);

    GeoPoint moveToward(GeoPoint origin, GeoPoint destination, double stepMeters);
}
