package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.enums.LondonZone;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;

class LondonTrafficBusinessRulesTest {

    @Test
    void shouldApplyStatusScenarioAndZoneTrafficMultipliers() {
        DriverLocationGeneratorProperties properties = new DriverLocationGeneratorProperties();
        DriverLocationGeneratorProperties.ScenarioConfig config = new DriverLocationGeneratorProperties.ScenarioConfig();
        config.setTrafficMultiplier(0.9d);
        properties.setScenarios(new EnumMap<>(DriverLocationGeneratorProperties.ScenarioKey.class));
        properties.getScenarios().put(DriverLocationGeneratorProperties.ScenarioKey.CONCERT_RAIN, config);

        double step = LondonTrafficBusinessRules.stepMeters(
                DriverGeoState.builder()
                        .status(DriverStatus.REPOSITIONING)
                        .scenario(LondonScenario.CONCERT_RAIN)
                        .currentZone(LondonZone.WEMBLEY_EVENT_ZONE)
                        .build(),
                properties);

        assertThat(step).isEqualTo(properties.getRepositionStepMeters() * 0.9d * 0.72d);
        assertThat(LondonTrafficBusinessRules.stepMeters(
                DriverGeoState.builder()
                        .status(DriverStatus.OFFLINE)
                        .currentZone(LondonZone.CENTRAL_LONDON)
                        .build(),
                properties)).isZero();
    }
}

