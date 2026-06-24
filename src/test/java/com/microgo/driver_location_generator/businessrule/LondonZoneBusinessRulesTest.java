package com.microgo.driver_location_generator.businessrule;

import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonZone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LondonZoneBusinessRulesTest {

    @Test
    void shouldResolveLondonZonesIncludingBoundaryPriority() {
        assertThat(LondonZoneBusinessRules.resolveZone(point(51.5560, -0.2796))).isEqualTo(LondonZone.WEMBLEY_EVENT_ZONE);
        assertThat(LondonZoneBusinessRules.resolveZone(point(51.4700, -0.4543))).isEqualTo(LondonZone.HEATHROW_CORRIDOR);
        assertThat(LondonZoneBusinessRules.resolveZone(point(51.5074, -0.1278))).isEqualTo(LondonZone.CENTRAL_LONDON);
        assertThat(LondonZoneBusinessRules.resolveZone(point(52.0, 0.1))).isEqualTo(LondonZone.GENERAL_LONDON);
        assertThat(LondonZoneBusinessRules.resolveZone(point(51.5759, -0.2796))).isEqualTo(LondonZone.WEMBLEY_EVENT_ZONE);
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}

