package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.GeoPoint;

public final class DriverSeedBusinessRules {

    private static final double LATITUDE_KILOMETERS_PER_DEGREE = 55.0d;
    private static final double LONGITUDE_KILOMETERS_PER_DEGREE = 35.0d;
    private static final int POSITION_BUCKET_COUNT = 1_000;
    private static final int LONGITUDE_HASH_DIVISOR = 31;

    private DriverSeedBusinessRules() {
    }

    public static GeoPoint initialPosition(
            String driverId,
            DriverLocationGeneratorProperties.ScenarioConfig config,
            double defaultLatitude,
            double defaultLongitude) {
        if (config == null) {
            return point(defaultLatitude, defaultLongitude);
        }

        long hash = Math.abs((long) driverId.hashCode());
        double latOffset = centeredBucket(hash) * config.getSpreadKm() / LATITUDE_KILOMETERS_PER_DEGREE;
        double lonOffset = centeredBucket(hash / LONGITUDE_HASH_DIVISOR)
                * config.getSpreadKm()
                / LONGITUDE_KILOMETERS_PER_DEGREE;
        return point(config.getCenterLatitude() + latOffset, config.getCenterLongitude() + lonOffset);
    }

    private static double centeredBucket(long hash) {
        return (hash % POSITION_BUCKET_COUNT) / (double) POSITION_BUCKET_COUNT - 0.5d;
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}

