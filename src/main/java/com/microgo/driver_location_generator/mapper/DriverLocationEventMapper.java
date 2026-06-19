package com.microgo.driver_location_generator.mapper;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.kafka.model.DriverBecameAvailableEvent;
import com.microgo.driver_location_generator.kafka.model.DriverEnteredZoneEvent;
import com.microgo.driver_location_generator.kafka.model.DriverLocationUpdatedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverReachedDestinationEvent;
import com.microgo.driver_location_generator.kafka.model.DriverReachedPickupEvent;
import lombok.experimental.UtilityClass;

import java.time.Instant;

@UtilityClass
public class DriverLocationEventMapper {

    public DriverLocationUpdatedEvent toDriverLocationUpdatedEvent(DriverGeoState state) {
        return DriverLocationUpdatedEvent.builder()
                .driverId(state.getDriverId())
                .driverIdentifier(state.getDriverId())
                .providerIdentifier(state.getDriverId())
                .driverDisplayId(state.getDriverDisplayId())
                .scenario(state.getScenario())
                .status(state.getStatus())
                .zone(state.getCurrentZone())
                .latitude(state.getCurrentPosition().getLatitude())
                .longitude(state.getCurrentPosition().getLongitude())
                .available(state.isAvailable())
                .tickSequence(state.getTickSequence())
                .occurredAt(state.getUpdatedAt())
                .build();
    }

    public DriverEnteredZoneEvent toDriverEnteredZoneEvent(DriverGeoState state, Instant occurredAt) {
        return DriverEnteredZoneEvent.builder()
                .driverId(state.getDriverId())
                .zone(state.getCurrentZone())
                .occurredAt(occurredAt)
                .build();
    }

    public DriverReachedPickupEvent toDriverReachedPickupEvent(String driverId, String rideId, Instant occurredAt) {
        return DriverReachedPickupEvent.builder()
                .driverId(driverId)
                .rideId(rideId)
                .occurredAt(occurredAt)
                .build();
    }

    public DriverReachedDestinationEvent toDriverReachedDestinationEvent(
            String driverId,
            String rideId,
            Instant occurredAt) {
        return DriverReachedDestinationEvent.builder()
                .driverId(driverId)
                .rideId(rideId)
                .occurredAt(occurredAt)
                .build();
    }

    public DriverBecameAvailableEvent toDriverBecameAvailableEvent(String driverId, Instant occurredAt) {
        return DriverBecameAvailableEvent.builder()
                .driverId(driverId)
                .occurredAt(occurredAt)
                .build();
    }
}
