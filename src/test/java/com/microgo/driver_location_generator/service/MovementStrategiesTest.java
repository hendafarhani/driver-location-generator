package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.service.serviceimpl.LondonDistanceMatrixImpl;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.IdleMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.PickupMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.RepositioningMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.ScenarioMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.TripMovementStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;

class MovementStrategiesTest {

    private DriverLocationGeneratorProperties properties;
    private LondonDistanceMatrix distanceMatrix;

    @BeforeEach
    void setUp() {
        properties = new DriverLocationGeneratorProperties();
        properties.setArrivalThresholdMeters(35.0d);
        DriverLocationGeneratorProperties.ScenarioConfig scenario =
                new DriverLocationGeneratorProperties.ScenarioConfig();
        scenario.setDemandBiasLatitude(51.5560);
        scenario.setDemandBiasLongitude(-0.2796);
        properties.setScenarios(new EnumMap<>(DriverLocationGeneratorProperties.ScenarioKey.class));
        properties.getScenarios().put(DriverLocationGeneratorProperties.ScenarioKey.CONCERT_RAIN, scenario);
        distanceMatrix = new LondonDistanceMatrixImpl();
    }

    @Test
    void idleStrategyStartsCruising() {
        IdleMovementStrategy strategy = new IdleMovementStrategy(distanceMatrix);
        DriverGeoState state = state(DriverStatus.IDLE).toBuilder().plannedStepMeters(100).build();

        MovementUpdate update = strategy.move(state);

        assertThat(strategy.supports(state)).isTrue();
        assertThat(update.getNewState().getStatus()).isEqualTo(DriverStatus.CRUISING);
        assertThat(update.getNewState().getCurrentPosition()).isNotEqualTo(state.getCurrentPosition());
    }

    @Test
    void repositioningStrategyMakesDriverAvailableAtTarget() {
        RepositioningMovementStrategy strategy = new RepositioningMovementStrategy(properties, distanceMatrix);
        GeoPoint target = point(51.5001, -0.1201);
        DriverGeoState state = state(DriverStatus.REPOSITIONING).toBuilder()
                .targetPosition(target)
                .plannedStepMeters(100)
                .build();

        MovementUpdate update = strategy.move(state);

        assertThat(update.isBecameAvailable()).isTrue();
        assertThat(update.getNewState().getStatus()).isEqualTo(DriverStatus.IDLE);
        assertThat(update.getNewState().isAvailable()).isTrue();
        assertThat(update.getNewState().getTargetPosition()).isNull();
        assertThat(update.getNewState().getCurrentPosition()).isEqualTo(target);
    }

    @Test
    void pickupStrategySignalsArrivalAndWaitsForPassenger() {
        PickupMovementStrategy strategy = new PickupMovementStrategy(properties, distanceMatrix);
        GeoPoint pickup = point(51.5001, -0.1201);
        DriverGeoState state = state(DriverStatus.MOVING_TO_PICKUP).toBuilder()
                .pickupPosition(pickup)
                .activeRideId("ride-1")
                .plannedStepMeters(100)
                .build();

        MovementUpdate update = strategy.move(state);

        assertThat(update.isReachedPickup()).isTrue();
        assertThat(update.getRideId()).isEqualTo("ride-1");
        assertThat(update.getNewState().getStatus()).isEqualTo(DriverStatus.WAITING_FOR_PASSENGER);
        assertThat(update.getNewState().getCurrentPosition()).isEqualTo(pickup);
    }

    @Test
    void tripStrategyClearsRideStateAtDestination() {
        TripMovementStrategy strategy = new TripMovementStrategy(properties, distanceMatrix);
        GeoPoint destination = point(51.5001, -0.1201);
        DriverGeoState state = state(DriverStatus.ON_TRIP).toBuilder()
                .activeRideId("ride-1")
                .pickupPosition(point(51.49, -0.11))
                .dropoffPosition(destination)
                .plannedStepMeters(100)
                .build();

        MovementUpdate update = strategy.move(state);

        assertThat(update.isReachedDestination()).isTrue();
        assertThat(update.isBecameAvailable()).isTrue();
        assertThat(update.getRideId()).isEqualTo("ride-1");
        assertThat(update.getNewState().getStatus()).isEqualTo(DriverStatus.IDLE);
        assertThat(update.getNewState().getActiveRideId()).isNull();
        assertThat(update.getNewState().getPickupPosition()).isNull();
        assertThat(update.getNewState().getDropoffPosition()).isNull();
    }

    @Test
    void scenarioStrategyMovesCruisingDriverTowardDemandBias() {
        ScenarioMovementStrategy strategy = new ScenarioMovementStrategy(properties, distanceMatrix);
        DriverGeoState state = state(DriverStatus.CRUISING).toBuilder()
                .scenario(LondonScenario.CONCERT_RAIN)
                .plannedStepMeters(100)
                .build();
        double distanceBefore = distanceMatrix.distanceMeters(
                state.getCurrentPosition(),
                point(51.5560, -0.2796));

        MovementUpdate update = strategy.move(state);

        assertThat(strategy.supports(state)).isTrue();
        assertThat(distanceMatrix.distanceMeters(
                update.getNewState().getCurrentPosition(),
                point(51.5560, -0.2796))).isLessThan(distanceBefore);
    }

    @Test
    void strategiesRejectStatesMissingRequiredStatusOrTarget() {
        assertThat(new RepositioningMovementStrategy(properties, distanceMatrix)
                .supports(state(DriverStatus.REPOSITIONING))).isFalse();
        assertThat(new PickupMovementStrategy(properties, distanceMatrix)
                .supports(state(DriverStatus.MOVING_TO_PICKUP))).isFalse();
        assertThat(new TripMovementStrategy(properties, distanceMatrix)
                .supports(state(DriverStatus.ON_TRIP))).isFalse();
        assertThat(new ScenarioMovementStrategy(properties, distanceMatrix)
                .supports(state(DriverStatus.CRUISING))).isFalse();
    }

    private static DriverGeoState state(DriverStatus status) {
        return DriverGeoState.builder()
                .driverId("driver-1")
                .status(status)
                .currentPosition(point(51.5000, -0.1200))
                .build();
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}
