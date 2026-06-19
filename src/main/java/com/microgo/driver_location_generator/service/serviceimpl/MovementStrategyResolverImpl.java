package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.service.MovementStrategyResolver;
import com.microgo.driver_location_generator.service.strategy.DriverMovementStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MovementStrategyResolverImpl implements MovementStrategyResolver {

    private final List<DriverMovementStrategy> strategies;

    public MovementStrategyResolverImpl(List<DriverMovementStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public DriverMovementStrategy resolve(DriverGeoState state) {
        return findFirstSupportingStrategy(state)
                .orElseThrow(() -> createMissingStrategyException(state));
    }

    private Optional<DriverMovementStrategy> findFirstSupportingStrategy(DriverGeoState state) {
        return strategies.stream()
                .filter(strategy -> supportsState(strategy, state))
                .findFirst();
    }

    private boolean supportsState(DriverMovementStrategy strategy, DriverGeoState state) {
        return strategy.supports(state);
    }

    private IllegalStateException createMissingStrategyException(DriverGeoState state) {
        return new IllegalStateException(
                "No movement strategy found for driver status " + state.getStatus()
        );
    }
}
