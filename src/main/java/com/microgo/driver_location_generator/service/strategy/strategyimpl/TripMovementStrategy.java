package com.microgo.driver_location_generator.service.strategy.strategyimpl;

import com.microgo.driver_location_generator.businessrule.MovementProgressBusinessRules;
import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.mapper.DriverMovementStrategyMapper;
import com.microgo.driver_location_generator.service.LondonDistanceMatrix;
import com.microgo.driver_location_generator.service.MovementUpdate;
import com.microgo.driver_location_generator.service.strategy.DriverMovementStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class TripMovementStrategy implements DriverMovementStrategy {

    private final DriverLocationGeneratorProperties properties;
    private final LondonDistanceMatrix distanceMatrix;

    @Override
    public boolean supports(DriverGeoState state) {
        return isActiveTripWithKnownDropOff(state);
    }

    @Override
    public MovementUpdate move(DriverGeoState state) {
        boolean reachedDropOff = hasReachedDropoff(state);
        return DriverMovementStrategyMapper.toTripProgressUpdate(
                state,
                reachedDropOff,
                resolveTripProgressPosition(state, reachedDropOff));
    }

    private boolean isActiveTripWithKnownDropOff(DriverGeoState state) {
        return state.getStatus() == DriverStatus.ON_TRIP && state.getDropoffPosition() != null;
    }

    private GeoPoint resolveTripProgressPosition(DriverGeoState state, boolean reachedDropoff) {
        return MovementProgressBusinessRules.resolveTargetProgressPosition(
                state.getCurrentPosition(),
                state.getDropoffPosition(),
                state.getPlannedStepMeters(),
                reachedDropoff,
                distanceMatrix);
    }

    private boolean hasReachedDropoff(DriverGeoState state) {
        return MovementProgressBusinessRules.hasReachedTarget(
                state.getCurrentPosition(),
                state.getDropoffPosition(),
                distanceMatrix,
                properties.getArrivalThresholdMeters());
    }
}
