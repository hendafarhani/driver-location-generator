package com.microgo.driver_location_generator.mapper;

import com.microgo.driver_location_generator.domain.GeoPoint;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LondonDistanceMatrixMapper {

    public GeoPoint toGeoPoint(double latitude, double longitude) {
        return GeoPoint.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
