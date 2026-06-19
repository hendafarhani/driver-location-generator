package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.domain.DriverStatus;
import com.microgo.driver_location_generator.domain.LondonScenario;
import com.microgo.driver_location_generator.domain.LondonZone;
import com.microgo.driver_location_generator.service.LondonTrafficModel;
import org.springframework.stereotype.Service;

@Service
public class LondonTrafficModelImpl implements LondonTrafficModel {

    private static final double DEFAULT_TRAFFIC_MULTIPLIER = 1.0d;
    private static final double OFFLINE_TRAFFIC_MULTIPLIER = 0.0d;
    private static final double WEMBLEY_EVENT_ZONE_TRAFFIC_MULTIPLIER = 0.72d;
    private static final double HEATHROW_CORRIDOR_TRAFFIC_MULTIPLIER = 0.80d;
    private static final double CENTRAL_LONDON_TRAFFIC_MULTIPLIER = 0.85d;

    private final DriverLocationGeneratorProperties properties;

    public LondonTrafficModelImpl(DriverLocationGeneratorProperties properties) {
        this.properties = properties;
    }

    @Override
    public double stepMeters(DriverGeoState state) {
        double baseStepMeters = getBaseStepMeters(state.getStatus());
        double scenarioTrafficMultiplier = getScenarioTrafficMultiplier(state.getScenario());
        double zoneTrafficMultiplier = getZoneTrafficMultiplier(state.getCurrentZone(), state.getStatus());

        return baseStepMeters * scenarioTrafficMultiplier * zoneTrafficMultiplier;
    }

    private double getBaseStepMeters(DriverStatus status) {
        return switch (status) {
            case REPOSITIONING -> properties.getRepositionStepMeters();
            case MOVING_TO_PICKUP -> properties.getPickupStepMeters();
            case ON_TRIP -> properties.getTripStepMeters();
            case IDLE, CRUISING, WAITING_FOR_PASSENGER -> properties.getIdleStepMeters();
            case OFFLINE -> 0.0d;
        };
    }

    private double getScenarioTrafficMultiplier(LondonScenario scenario) {
        if (scenario == null) {
            return DEFAULT_TRAFFIC_MULTIPLIER;
        }

        DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig = getScenarioConfig(scenario);
        return scenarioConfig == null
                ? DEFAULT_TRAFFIC_MULTIPLIER
                : scenarioConfig.getTrafficMultiplier();
    }

    private DriverLocationGeneratorProperties.ScenarioConfig getScenarioConfig(LondonScenario scenario) {
        DriverLocationGeneratorProperties.ScenarioKey scenarioKey =
                DriverLocationGeneratorProperties.ScenarioKey.valueOf(scenario.name());
        return properties.getScenarios().get(scenarioKey);
    }

    private double getZoneTrafficMultiplier(LondonZone zone, DriverStatus status) {
        if (isDriverOffline(status)) {
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

    private static boolean isDriverOffline(DriverStatus status) {
        return status == DriverStatus.OFFLINE;
    }
}
