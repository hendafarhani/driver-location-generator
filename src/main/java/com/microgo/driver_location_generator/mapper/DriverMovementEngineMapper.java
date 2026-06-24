package com.microgo.driver_location_generator.mapper;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.entity.DriverProfileEntity;
import com.microgo.driver_location_generator.enums.LondonZone;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.service.MovementUpdate;
import lombok.experimental.UtilityClass;

import java.time.Instant;

@UtilityClass
public class DriverMovementEngineMapper {

    public DriverGeoState toRegisteredDriverState(
            DriverProfileEntity profile,
            LondonScenario scenario,
            double idleStepMeters,
            GeoPoint initialPosition,
            LondonZone currentZone,
            Instant updatedAt) {
        return DriverGeoState.builder()
                .driverId(profile.getDriverIdentifier())
                .driverDisplayId(profile.getDriverDisplayId())
                .scenario(scenario)
                .status(DriverStatus.IDLE)
                .available(true)
                .plannedStepMeters(idleStepMeters)
                .tickSequence(0L)
                .currentPosition(initialPosition)
                .currentZone(currentZone)
                .updatedAt(updatedAt)
                .build();
    }

    public DriverGeoState toRepositioningCommandState(DriverGeoState state, RepositionDriverCommand command, GeoPoint targetPosition) {
        return state.toBuilder()
                .status(DriverStatus.REPOSITIONING)
                .available(false)
                .targetPosition(targetPosition)
                .build();
    }

    public DriverGeoState toPickupCommandState(DriverGeoState state, MoveToPickupCommand command, GeoPoint pickupPosition) {
        return state.toBuilder()
                .activeRideId(command.getRideId())
                .status(DriverStatus.MOVING_TO_PICKUP)
                .available(false)
                .pickupPosition(pickupPosition)
                .build();
    }

    public DriverGeoState toTripCommandState(DriverGeoState state, StartTripCommand command, GeoPoint dropoffPosition) {
        return state.toBuilder()
                .activeRideId(command.getRideId())
                .status(DriverStatus.ON_TRIP)
                .available(false)
                .dropoffPosition(dropoffPosition)
                .build();
    }

    public DriverGeoState toStoppedDriverState(DriverGeoState state) {
        return state.toBuilder()
                .status(DriverStatus.OFFLINE)
                .available(false)
                .targetPosition(null)
                .pickupPosition(null)
                .dropoffPosition(null)
                .activeRideId(null)
                .build();
    }

    public DriverGeoState toFreedDriverState(DriverGeoState state) {
        return state.toBuilder()
                .status(DriverStatus.IDLE)
                .available(true)
                .targetPosition(null)
                .pickupPosition(null)
                .dropoffPosition(null)
                .activeRideId(null)
                .build();
    }

    public MovementUpdate toUnchangedMovementUpdate(DriverGeoState state) {
        return MovementUpdate.builder()
                .newState(state)
                .build();
    }

    public DriverGeoState toStepAwareState(DriverGeoState state, double plannedStepMeters) {
        return state.toBuilder()
                .plannedStepMeters(plannedStepMeters)
                .build();
    }

    public DriverGeoState toAdvancedDriverState(
            DriverGeoState existingState,
            DriverGeoState rawState,
            long nextTickSequence,
            Instant updatedAt,
            LondonZone currentZone) {
        return rawState.toBuilder()
                .tickSequence(nextTickSequence)
                .updatedAt(updatedAt)
                .currentZone(currentZone)
                .build();
    }

    public DriverGeoState toNoMovementAdvancedState(DriverGeoState existingState, long nextTickSequence, Instant updatedAt) {
        return existingState.toBuilder()
                .tickSequence(nextTickSequence)
                .updatedAt(updatedAt)
                .build();
    }

    public MovementUpdate toAdvancedMovementUpdate(DriverGeoState newState, boolean zoneChanged, MovementUpdate rawUpdate) {
        return MovementUpdate.builder()
                .newState(newState)
                .rideId(rawUpdate.getRideId())
                .zoneChanged(zoneChanged)
                .reachedPickup(rawUpdate.isReachedPickup())
                .reachedDestination(rawUpdate.isReachedDestination())
                .becameAvailable(rawUpdate.isBecameAvailable())
                .build();
    }

    public DriverGeoState toMutatedDriverState(
            DriverGeoState state,
            Instant updatedAt,
            LondonZone currentZone,
            double plannedStepMeters) {
        return state.toBuilder()
                .updatedAt(updatedAt)
                .currentZone(currentZone)
                .plannedStepMeters(plannedStepMeters)
                .build();
    }

    public GeoPoint toGeoPoint(double latitude, double longitude) {
        return GeoPoint.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
