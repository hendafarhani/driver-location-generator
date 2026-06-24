package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DriverSeedBusinessRulesTest {

    @Test
    void shouldUseDefaultCentralLondonPositionWhenScenarioConfigMissing() {
        GeoPoint position = DriverSeedBusinessRules.initialPosition("driver-1", null, 51.5074, -0.1278);

        assertThat(position.getLatitude()).isEqualTo(51.5074);
        assertThat(position.getLongitude()).isEqualTo(-0.1278);
    }

    @Test
    void shouldKeepMinimumIntegerHashDriverInsideScenarioSpread() {
        DriverLocationGeneratorProperties.ScenarioConfig config = new DriverLocationGeneratorProperties.ScenarioConfig();
        config.setCenterLatitude(51.5560);
        config.setCenterLongitude(-0.2796);
        config.setSpreadKm(3.0);

        GeoPoint position = DriverSeedBusinessRules.initialPosition(
                "polygenelubricants",
                config,
                51.5074,
                -0.1278);

        assertThat(position.getLatitude()).isBetween(51.52, 51.59);
        assertThat(position.getLongitude()).isBetween(-0.33, -0.23);
    }
}

