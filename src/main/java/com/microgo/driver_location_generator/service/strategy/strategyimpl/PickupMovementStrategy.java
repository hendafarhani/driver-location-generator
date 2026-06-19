package com.microgo.driver_location_generator.service.strategy.strategyimpl;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.domain.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.mapper.DriverMovementStrategyMapper;
import com.microgo.driver_location_generator.service.LondonDistanceMatrix;
import com.microgo.driver_location_generator.service.MovementUpdate;
import com.microgo.driver_location_generator.service.strategy.DriverMovementStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class PickupMovementStrategy implements DriverMovementStrategy {

    private final DriverLocationGeneratorProperties properties;
    private final LondonDistanceMatrix distanceMatrix;

    @Override
    public boolean supports(DriverGeoState state) {
        return isMovingToPickupWithKnownPickupPosition(state);
    }

    @Override
    public MovementUpdate move(DriverGeoState state) {
        boolean reachedPickup = hasReachedPickup(state);
        return DriverMovementStrategyMapper.toPickupProgressUpdate(
                state,
                reachedPickup,
                resolvePickupProgressPosition(state, reachedPickup));
    }

    private boolean isMovingToPickupWithKnownPickupPosition(DriverGeoState state) {
        return state.getStatus() == DriverStatus.MOVING_TO_PICKUP && state.getPickupPosition() != null;
    }

    private GeoPoint resolvePickupProgressPosition(DriverGeoState state, boolean reachedPickup) {
        if (reachedPickup) {
            return state.getPickupPosition();
        }
        return distanceMatrix.moveToward(
                state.getCurrentPosition(),
                state.getPickupPosition(),
                state.getPlannedStepMeters());
    }

    private boolean hasReachedPickup(DriverGeoState state) {
        return calculateRemainingDistanceToPickup(state) <= properties.getArrivalThresholdMeters();
    }

    private double calculateRemainingDistanceToPickup(DriverGeoState state) {
        return distanceMatrix.distanceMeters(state.getCurrentPosition(), state.getPickupPosition());
    }
}
