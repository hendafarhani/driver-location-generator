package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.enums.LondonZone;

public final class LondonTrafficBusinessRules {

    private static final double DEFAULT_TRAFFIC_MULTIPLIER = 1.0d;
    private static final double OFFLINE_TRAFFIC_MULTIPLIER = 0.0d;
    private static final double WEMBLEY_EVENT_ZONE_TRAFFIC_MULTIPLIER = 0.72d;
    private static final double HEATHROW_CORRIDOR_TRAFFIC_MULTIPLIER = 0.80d;
    private static final double CENTRAL_LONDON_TRAFFIC_MULTIPLIER = 0.85d;

    private LondonTrafficBusinessRules() {
    }

    public static double stepMeters(DriverGeoState state, DriverLocationGeneratorProperties properties) {
        double baseStepMeters = baseStepMeters(state.getStatus(), properties);
        double scenarioTrafficMultiplier = scenarioTrafficMultiplier(state.getScenario(), properties);
        double zoneTrafficMultiplier = zoneTrafficMultiplier(state.getCurrentZone(), state.getStatus());

        return baseStepMeters * scenarioTrafficMultiplier * zoneTrafficMultiplier;
    }

    private static double baseStepMeters(DriverStatus status, DriverLocationGeneratorProperties properties) {
        return switch (status) {
            case REPOSITIONING -> properties.getRepositionStepMeters();
            case MOVING_TO_PICKUP -> properties.getPickupStepMeters();
            case ON_TRIP -> properties.getTripStepMeters();
            case IDLE, CRUISING, WAITING_FOR_PASSENGER -> properties.getIdleStepMeters();
            case OFFLINE -> 0.0d;
        };
    }

    private static double scenarioTrafficMultiplier(
            LondonScenario scenario,
            DriverLocationGeneratorProperties properties) {
        if (scenario == null) {
            return DEFAULT_TRAFFIC_MULTIPLIER;
        }

        DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig = properties.getScenarios()
                .get(DriverLocationGeneratorProperties.ScenarioKey.valueOf(scenario.name()));
        return scenarioConfig == null ? DEFAULT_TRAFFIC_MULTIPLIER : scenarioConfig.getTrafficMultiplier();
    }

    private static double zoneTrafficMultiplier(LondonZone zone, DriverStatus status) {
        if (status == DriverStatus.OFFLINE) {
            return OFFLINE_TRAFFIC_MULTIPLIER;
        }
        if (zone == null) {
            return DEFAULT_TRAFFIC_MULTIPLIER;
        }

        return switch (zone) {
            case WEMBLEY_EVENT_ZONE -> WEMBLEY_EVENT_ZONE_TRAFFIC_MULTIPLIER;
            case HEATHROW_CORRIDOR -> HEATHROW_CORRIDOR_TRAFFIC_MULTIPLIER;
            case CENTRAL_LONDON -> CENTRAL_LONDON_TRAFFIC_MULTIPLIER;
            case GENERAL_LONDON -> DEFAULT_TRAFFIC_MULTIPLIER;
        };
    }
}

