package com.microgo.driver_location_generator.service.strategy.strategyimpl;

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
@Order(40)
@RequiredArgsConstructor
public class ScenarioMovementStrategy implements DriverMovementStrategy {

    private final DriverLocationGeneratorProperties properties;
    private final LondonDistanceMatrix distanceMatrix;

    @Override
    public boolean supports(DriverGeoState state) {
        return isCruisingInScenarioAwareMode(state);
    }

    @Override
    public MovementUpdate move(DriverGeoState state) {
        DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig = findScenarioConfig(state);
        if (scenarioConfig == null) {
            return DriverMovementStrategyMapper.toUnchangedUpdate(state);
        }

        return DriverMovementStrategyMapper.toScenarioBiasedCruisingUpdate(
                state,
                calculateScenarioBiasedPosition(state, scenarioConfig));
    }

    private boolean isCruisingInScenarioAwareMode(DriverGeoState state) {
        return state.getStatus() == DriverStatus.CRUISING && state.getScenario() != null;
    }

    private DriverLocationGeneratorProperties.ScenarioConfig findScenarioConfig(DriverGeoState state) {
        return properties.getScenarios().get(DriverLocationGeneratorProperties.ScenarioKey.valueOf(state.getScenario().name()));
    }

    private GeoPoint calculateScenarioBiasedPosition(
            DriverGeoState state,
            DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig) {
        return distanceMatrix.moveToward(
                state.getCurrentPosition(),
                buildScenarioDemandBiasTarget(scenarioConfig),
                state.getPlannedStepMeters());
    }

    private GeoPoint buildScenarioDemandBiasTarget(DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig) {
        return DriverMovementStrategyMapper.toGeoPoint(
                scenarioConfig.getDemandBiasLatitude(),
                scenarioConfig.getDemandBiasLongitude());
    }
}
