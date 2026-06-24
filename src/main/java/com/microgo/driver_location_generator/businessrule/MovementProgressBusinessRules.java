package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.service.LondonDistanceMatrix;

public final class MovementProgressBusinessRules {

    private static final double LATITUDE_METERS_PER_DEGREE = 111_320.0d;

    private MovementProgressBusinessRules() {
    }

    public static boolean hasReachedTarget(
            GeoPoint currentPosition,
            GeoPoint targetPosition,
            LondonDistanceMatrix distanceMatrix,
            double arrivalThresholdMeters) {
        return distanceMatrix.distanceMeters(currentPosition, targetPosition) <= arrivalThresholdMeters;
    }

    public static GeoPoint resolveTargetProgressPosition(
            GeoPoint currentPosition,
            GeoPoint targetPosition,
            double plannedStepMeters,
            boolean reachedTarget,
            LondonDistanceMatrix distanceMatrix) {
        if (reachedTarget) {
            return targetPosition;
        }
        return distanceMatrix.moveToward(currentPosition, targetPosition, plannedStepMeters);
    }

    public static GeoPoint buildCruisingDriftTarget(GeoPoint currentPosition, double plannedStepMeters) {
        double latitudeOffset = plannedStepMeters / LATITUDE_METERS_PER_DEGREE;
        return point(
                currentPosition.getLatitude() + latitudeOffset,
                currentPosition.getLongitude() + (latitudeOffset / 2));
    }

    public static GeoPoint buildScenarioDemandBiasTarget(
            DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig) {
        return point(
                scenarioConfig.getDemandBiasLatitude(),
                scenarioConfig.getDemandBiasLongitude());
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}

