package com.microgo.driver_location_generator.mapper;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.domain.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.domain.LondonScenario;
import com.microgo.driver_location_generator.domain.LondonZone;
import com.microgo.driver_location_generator.kafka.model.DriverLocationUpdatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLocationEventMapperTest {

    @Test
    void mapsCompleteLocationSnapshot() {
        Instant updatedAt = Instant.parse("2026-06-19T12:00:00Z");
        DriverGeoState state = DriverGeoState.builder()
                .driverId("driver-1")
                .driverDisplayId("DRV-1")
                .scenario(LondonScenario.AIRPORT_RUSH)
                .status(DriverStatus.REPOSITIONING)
                .currentZone(LondonZone.HEATHROW_CORRIDOR)
                .currentPosition(GeoPoint.builder().latitude(51.47).longitude(-0.4543).build())
                .available(false)
                .tickSequence(12)
                .updatedAt(updatedAt)
                .build();

        DriverLocationUpdatedEvent event =
                DriverLocationEventMapper.toDriverLocationUpdatedEvent(state);

        assertThat(event)
                .usingRecursiveComparison()
                .isEqualTo(DriverLocationUpdatedEvent.builder()
                        .driverId("driver-1")
                        .driverIdentifier("driver-1")
                        .providerIdentifier("driver-1")
                        .driverDisplayId("DRV-1")
                        .scenario(LondonScenario.AIRPORT_RUSH)
                        .status(DriverStatus.REPOSITIONING)
                        .zone(LondonZone.HEATHROW_CORRIDOR)
                        .latitude(51.47)
                        .longitude(-0.4543)
                        .available(false)
                        .tickSequence(12)
                        .occurredAt(updatedAt)
                        .build());
    }

    @Test
    void mapsLifecycleEventArguments() {
        Instant occurredAt = Instant.parse("2026-06-19T12:00:00Z");
        DriverGeoState state = DriverGeoState.builder()
                .driverId("driver-1")
                .currentZone(LondonZone.CENTRAL_LONDON)
                .build();

        assertThat(DriverLocationEventMapper.toDriverEnteredZoneEvent(state, occurredAt).getZone())
                .isEqualTo(LondonZone.CENTRAL_LONDON);
        assertThat(DriverLocationEventMapper
                .toDriverReachedPickupEvent("driver-1", "ride-1", occurredAt).getRideId())
                .isEqualTo("ride-1");
        assertThat(DriverLocationEventMapper
                .toDriverReachedDestinationEvent("driver-1", "ride-1", occurredAt).getRideId())
                .isEqualTo("ride-1");
        assertThat(DriverLocationEventMapper
                .toDriverBecameAvailableEvent("driver-1", occurredAt).getOccurredAt())
                .isEqualTo(occurredAt);
    }
}
