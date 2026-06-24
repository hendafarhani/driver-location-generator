package com.microgo.driver_location_generator.service.strategy.strategyimpl;

import com.microgo.driver_location_generator.businessrule.MovementProgressBusinessRules;
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
@Order(50)
@RequiredArgsConstructor
public class IdleMovementStrategy implements DriverMovementStrategy {

    private final LondonDistanceMatrix distanceMatrix;

    @Override
    public boolean supports(DriverGeoState state) {
        return isIdleOrWaitingForPassenger(state);
    }

    @Override
    public MovementUpdate move(DriverGeoState state) {
        return DriverMovementStrategyMapper.toIdleCruisingUpdate(state, calculateCruisingPosition(state));
    }

    private GeoPoint calculateCruisingPosition(DriverGeoState state) {
        return distanceMatrix.moveToward(
                state.getCurrentPosition(),
                MovementProgressBusinessRules.buildCruisingDriftTarget(
                        state.getCurrentPosition(),
                        state.getPlannedStepMeters()),
                state.getPlannedStepMeters());
    }


    private boolean isIdleOrWaitingForPassenger(DriverGeoState state) {
        return state.getStatus() == DriverStatus.IDLE
                || state.getStatus() == DriverStatus.WAITING_FOR_PASSENGER;
    }
}
