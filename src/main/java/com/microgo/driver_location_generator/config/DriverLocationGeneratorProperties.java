package com.microgo.driver_location_generator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "driver-location-generator")
public class DriverLocationGeneratorProperties {

    private long movementIntervalMs = 5_000L;
    private double idleStepMeters = 120.0;
    private double repositionStepMeters = 260.0;
    private double pickupStepMeters = 340.0;
    private double tripStepMeters = 420.0;
    private double arrivalThresholdMeters = 35.0;
    private Redis redis = new Redis();
    private Map<ScenarioKey, ScenarioConfig> scenarios = new EnumMap<>(ScenarioKey.class);

    @Getter
    @Setter
    public static class Redis {
        private String geoKey = "vehicle_location";
        private String stateKeyPrefix = "driver:geo-state:";
        private String zoneKeyPrefix = "driver:zone:";
    }

    @Getter
    @Setter
    public static class ScenarioConfig {
        private double centerLatitude;
        private double centerLongitude;
        private double spreadKm;
        private double demandBiasLatitude;
        private double demandBiasLongitude;
        private double trafficMultiplier = 1.0;
    }

    public enum ScenarioKey {
        CONCERT_RAIN,
        AIRPORT_RUSH
    }
}
