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
@Order(10)
@RequiredArgsConstructor
public class RepositioningMovementStrategy implements DriverMovementStrategy {

    private final DriverLocationGeneratorProperties properties;
    private final LondonDistanceMatrix distanceMatrix;

    @Override
    public boolean supports(DriverGeoState state) {
        return isRepositioningWithTarget(state);
    }

    @Override
    public MovementUpdate move(DriverGeoState state) {
        boolean reachedTarget = hasReachedRepositionTarget(state);
        return DriverMovementStrategyMapper.toRepositioningUpdate(
                state,
                reachedTarget,
                resolveRepositionedPosition(state, reachedTarget));
    }

    private boolean isRepositioningWithTarget(DriverGeoState state) {
        return state.getStatus() == DriverStatus.REPOSITIONING && state.hasTargetPosition();
    }

    private GeoPoint resolveRepositionedPosition(DriverGeoState state, boolean reachedTarget) {
        return MovementProgressBusinessRules.resolveTargetProgressPosition(
                state.getCurrentPosition(),
                state.getTargetPosition(),
                state.getPlannedStepMeters(),
                reachedTarget,
                distanceMatrix);
    }

    private boolean hasReachedRepositionTarget(DriverGeoState state) {
        return MovementProgressBusinessRules.hasReachedTarget(
                state.getCurrentPosition(),
                state.getTargetPosition(),
                distanceMatrix,
                properties.getArrivalThresholdMeters());
    }
}
