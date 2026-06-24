package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.service.LondonDistanceMatrix;
import com.microgo.driver_location_generator.service.serviceimpl.LondonDistanceMatrixImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovementProgressBusinessRulesTest {

    private final LondonDistanceMatrix distanceMatrix = new LondonDistanceMatrixImpl();

    @Test
    void shouldDetectArrivalAndSnapToTarget() {
        GeoPoint current = point(51.5000, -0.1200);
        GeoPoint target = point(51.5001, -0.1201);

        boolean reached = MovementProgressBusinessRules.hasReachedTarget(current, target, distanceMatrix, 35.0d);
        GeoPoint resolved = MovementProgressBusinessRules.resolveTargetProgressPosition(
                current,
                target,
                100.0d,
                reached,
                distanceMatrix);

        assertThat(reached).isTrue();
        assertThat(resolved).isEqualTo(target);
    }

    @Test
    void shouldBuildIdleDriftAndScenarioBiasTargets() {
        GeoPoint current = point(51.5000, -0.1200);
        GeoPoint driftTarget = MovementProgressBusinessRules.buildCruisingDriftTarget(current, 111.32d);

        DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig = new DriverLocationGeneratorProperties.ScenarioConfig();
        scenarioConfig.setDemandBiasLatitude(51.5560);
        scenarioConfig.setDemandBiasLongitude(-0.2796);
        GeoPoint scenarioTarget = MovementProgressBusinessRules.buildScenarioDemandBiasTarget(scenarioConfig);

        assertThat(driftTarget.getLatitude()).isCloseTo(51.5010, within(0.00001));
        assertThat(driftTarget.getLongitude()).isCloseTo(-0.1195, within(0.00001));
        assertThat(scenarioTarget).isEqualTo(point(51.5560, -0.2796));
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}

