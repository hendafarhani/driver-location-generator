package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.domain.GeoPoint;

public final class LondonDistanceBusinessRules {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private LondonDistanceBusinessRules() {
    }

    public static double distanceMeters(GeoPoint origin, GeoPoint destination) {
        double latitudeDifferenceRadians = Math.toRadians(destination.getLatitude() - origin.getLatitude());
        double longitudeDifferenceRadians = Math.toRadians(destination.getLongitude() - origin.getLongitude());
        double haversineValue = squareSineOfHalfAngle(latitudeDifferenceRadians)
                + Math.cos(Math.toRadians(origin.getLatitude()))
                * Math.cos(Math.toRadians(destination.getLatitude()))
                * squareSineOfHalfAngle(longitudeDifferenceRadians);
        double angularDistanceRadians = 2 * Math.atan2(Math.sqrt(haversineValue), Math.sqrt(1 - haversineValue));

        return EARTH_RADIUS_METERS * angularDistanceRadians;
    }

    public static GeoPoint moveToward(GeoPoint origin, GeoPoint destination, double stepMeters) {
        double distanceToDestinationMeters = distanceMeters(origin, destination);
        if (distanceToDestinationMeters == 0.0d || distanceToDestinationMeters <= stepMeters) {
            return destination;
        }

        double movementRatio = stepMeters / distanceToDestinationMeters;
        return point(
                interpolateCoordinate(origin.getLatitude(), destination.getLatitude(), movementRatio),
                interpolateCoordinate(origin.getLongitude(), destination.getLongitude(), movementRatio));
    }

    private static double squareSineOfHalfAngle(double angleRadians) {
        double sineOfHalfAngle = Math.sin(angleRadians / 2);
        return sineOfHalfAngle * sineOfHalfAngle;
    }

    private static double interpolateCoordinate(double origin, double destination, double movementRatio) {
        return origin + ((destination - origin) * movementRatio);
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}

