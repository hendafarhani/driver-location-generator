package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.domain.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LondonDistanceBusinessRulesTest {

    @Test
    void shouldCalculateKnownLondonDistanceAndPartialMovement() {
        GeoPoint origin = point(51.5074, -0.1278);
        GeoPoint destination = point(51.5174, -0.1278);

        double distance = LondonDistanceBusinessRules.distanceMeters(origin, destination);
        GeoPoint moved = LondonDistanceBusinessRules.moveToward(origin, destination, 100);

        assertThat(distance).isBetween(1_110.0, 1_113.0);
        assertThat(LondonDistanceBusinessRules.distanceMeters(origin, moved)).isCloseTo(100.0, within(0.2));
    }

    @Test
    void shouldReturnDestinationWhenStepCanReachIt() {
        GeoPoint origin = point(51.5074, -0.1278);
        GeoPoint destination = point(51.5075, -0.1278);

        assertThat(LondonDistanceBusinessRules.moveToward(origin, destination, 100)).isEqualTo(destination);
        assertThat(LondonDistanceBusinessRules.moveToward(origin, origin, 0)).isEqualTo(origin);
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}

