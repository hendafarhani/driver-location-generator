package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.enums.LondonZone;
import com.microgo.driver_location_generator.service.serviceimpl.LondonDistanceMatrixImpl;
import com.microgo.driver_location_generator.service.serviceimpl.LondonTrafficModelImpl;
import com.microgo.driver_location_generator.service.serviceimpl.LondonZoneServiceImpl;
import com.microgo.driver_location_generator.service.serviceimpl.MovementStrategyResolverImpl;
import com.microgo.driver_location_generator.service.strategy.DriverMovementStrategy;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreMovementServicesTest {

    @Test
    void calculatesKnownLondonDistanceAndPartialMovement() {
        LondonDistanceMatrix matrix = new LondonDistanceMatrixImpl();
        GeoPoint origin = point(51.5074, -0.1278);
        GeoPoint destination = point(51.5174, -0.1278);

        double distance = matrix.distanceMeters(origin, destination);
        GeoPoint moved = matrix.moveToward(origin, destination, 100);

        assertThat(distance).isBetween(1_110.0, 1_113.0);
        assertThat(matrix.distanceMeters(origin, moved)).isCloseTo(100.0, within(0.2));
    }

    @Test
    void returnsDestinationWhenStepCanReachIt() {
        LondonDistanceMatrix matrix = new LondonDistanceMatrixImpl();
        GeoPoint origin = point(51.5074, -0.1278);
        GeoPoint destination = point(51.5075, -0.1278);

        assertThat(matrix.moveToward(origin, destination, 100)).isSameAs(destination);
        assertThat(matrix.moveToward(origin, origin, 0)).isSameAs(origin);
    }

    @Test
    void resolvesLondonZonesIncludingBoundaryPriority() {
        LondonZoneService service = new LondonZoneServiceImpl();

        assertThat(service.resolveZone(point(51.5560, -0.2796))).isEqualTo(LondonZone.WEMBLEY_EVENT_ZONE);
        assertThat(service.resolveZone(point(51.4700, -0.4543))).isEqualTo(LondonZone.HEATHROW_CORRIDOR);
        assertThat(service.resolveZone(point(51.5074, -0.1278))).isEqualTo(LondonZone.CENTRAL_LONDON);
        assertThat(service.resolveZone(point(52.0, 0.1))).isEqualTo(LondonZone.GENERAL_LONDON);
        assertThat(service.resolveZone(point(51.5759, -0.2796))).isEqualTo(LondonZone.WEMBLEY_EVENT_ZONE);
    }

    @Test
    void appliesStatusScenarioAndZoneTrafficMultipliers() {
        DriverLocationGeneratorProperties properties = new DriverLocationGeneratorProperties();
        DriverLocationGeneratorProperties.ScenarioConfig config =
                new DriverLocationGeneratorProperties.ScenarioConfig();
        config.setTrafficMultiplier(0.9d);
        properties.setScenarios(new EnumMap<>(DriverLocationGeneratorProperties.ScenarioKey.class));
        properties.getScenarios().put(DriverLocationGeneratorProperties.ScenarioKey.CONCERT_RAIN, config);
        LondonTrafficModel model = new LondonTrafficModelImpl(properties);

        double step = model.stepMeters(DriverGeoState.builder()
                .status(DriverStatus.REPOSITIONING)
                .scenario(LondonScenario.CONCERT_RAIN)
                .currentZone(LondonZone.WEMBLEY_EVENT_ZONE)
                .build());

        assertThat(step).isEqualTo(properties.getRepositionStepMeters() * 0.9d * 0.72d);
        assertThat(model.stepMeters(DriverGeoState.builder()
                .status(DriverStatus.OFFLINE)
                .currentZone(LondonZone.CENTRAL_LONDON)
                .build())).isZero();
    }

    @Test
    void resolverUsesFirstSupportingStrategyAndReportsMissingStrategy() {
        DriverMovementStrategy unsupported = new StubStrategy(false);
        DriverMovementStrategy supported = new StubStrategy(true);
        MovementStrategyResolver resolver = new MovementStrategyResolverImpl(List.of(unsupported, supported));
        DriverGeoState state = DriverGeoState.builder().status(DriverStatus.IDLE).build();

        assertThat(resolver.resolve(state)).isSameAs(supported);
        assertThatThrownBy(() -> new MovementStrategyResolverImpl(List.of(unsupported)).resolve(state))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No movement strategy found for driver status IDLE");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }

    private record StubStrategy(boolean supported) implements DriverMovementStrategy {
        @Override
        public boolean supports(DriverGeoState state) {
            return supported;
        }

        @Override
        public MovementUpdate move(DriverGeoState state) {
            return MovementUpdate.builder().newState(state).build();
        }
    }
}
