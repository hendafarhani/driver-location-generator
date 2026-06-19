package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.mapper.LondonDistanceMatrixMapper;
import com.microgo.driver_location_generator.service.LondonDistanceMatrix;
import org.springframework.stereotype.Service;

@Service
public class LondonDistanceMatrixImpl implements LondonDistanceMatrix {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    @Override
    public double distanceMeters(GeoPoint origin, GeoPoint destination) {
        double latitudeDifferenceRadians = calculateLatitudeDifferenceRadians(origin, destination);
        double longitudeDifferenceRadians = calculateLongitudeDifferenceRadians(origin, destination);
        double haversineValue = calculateHaversineValue(
                origin,
                destination,
                latitudeDifferenceRadians,
                longitudeDifferenceRadians
        );
        double angularDistanceRadians = calculateAngularDistanceRadians(haversineValue);

        return convertAngularDistanceToMeters(angularDistanceRadians);
    }

    @Override
    public GeoPoint moveToward(GeoPoint origin, GeoPoint destination, double stepMeters) {
        double distanceToDestinationMeters = distanceMeters(origin, destination);
        if (canReachDestination(distanceToDestinationMeters, stepMeters)) {
            return destination;
        }

        double movementRatio = calculateMovementRatio(stepMeters, distanceToDestinationMeters);
        return interpolatePosition(origin, destination, movementRatio);
    }

    private double calculateLatitudeDifferenceRadians(GeoPoint origin, GeoPoint destination) {
        return Math.toRadians(destination.getLatitude() - origin.getLatitude());
    }

    private double calculateLongitudeDifferenceRadians(GeoPoint origin, GeoPoint destination) {
        return Math.toRadians(destination.getLongitude() - origin.getLongitude());
    }

    private double calculateHaversineValue(
            GeoPoint origin,
            GeoPoint destination,
            double latitudeDifferenceRadians,
            double longitudeDifferenceRadians
    ) {
        double latitudeComponent = squareSineOfHalfAngle(latitudeDifferenceRadians);
        double longitudeComponent = getLongitudeComponent(origin, destination, longitudeDifferenceRadians);

        return latitudeComponent + longitudeComponent;
    }

    private double getLongitudeComponent(GeoPoint origin, GeoPoint destination, double longitudeDifferenceRadians) {
        return Math.cos(Math.toRadians(origin.getLatitude()))
                * Math.cos(Math.toRadians(destination.getLatitude()))
                * squareSineOfHalfAngle(longitudeDifferenceRadians);
    }

    private double squareSineOfHalfAngle(double angleRadians) {
        double sineOfHalfAngle = Math.sin(angleRadians / 2);
        return sineOfHalfAngle * sineOfHalfAngle;
    }

    private double calculateAngularDistanceRadians(double haversineValue) {
        return 2 * Math.atan2(Math.sqrt(haversineValue), Math.sqrt(1 - haversineValue));
    }

    private double convertAngularDistanceToMeters(double angularDistanceRadians) {
        return EARTH_RADIUS_METERS * angularDistanceRadians;
    }

    private boolean canReachDestination(double distanceToDestinationMeters, double stepMeters) {
        return distanceToDestinationMeters == 0.0d || distanceToDestinationMeters <= stepMeters;
    }

    private double calculateMovementRatio(double stepMeters, double distanceToDestinationMeters) {
        return stepMeters / distanceToDestinationMeters;
    }

    private GeoPoint interpolatePosition(GeoPoint origin, GeoPoint destination, double movementRatio) {
        double latitude = interpolateCoordinate(
                origin.getLatitude(),
                destination.getLatitude(),
                movementRatio
        );
        double longitude = interpolateCoordinate(
                origin.getLongitude(),
                destination.getLongitude(),
                movementRatio
        );

        return LondonDistanceMatrixMapper.toGeoPoint(latitude, longitude);
    }

    private double interpolateCoordinate(double origin, double destination, double movementRatio) {
        return origin + ((destination - origin) * movementRatio);
    }
}
