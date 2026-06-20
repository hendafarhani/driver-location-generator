package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonZone;

public interface LondonZoneService {

    LondonZone resolveZone(GeoPoint point);
}
