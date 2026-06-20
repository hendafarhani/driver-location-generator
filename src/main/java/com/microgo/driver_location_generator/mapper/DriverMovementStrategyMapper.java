package com.microgo.driver_location_generator.mapper;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.service.MovementUpdate;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DriverMovementStrategyMapper {

    public MovementUpdate toIdleCruisingUpdate(DriverGeoState state, GeoPoint cruisingPosition) {
        return MovementUpdate.builder()
                .newState(state.toBuilder()
                        .status(DriverStatus.CRUISING)
                        .currentPosition(cruisingPosition)
                        .build())
                .build();
    }

    public MovementUpdate toPickupProgressUpdate(DriverGeoState state, boolean reachedPickup, GeoPoint nextPosition) {
        return MovementUpdate.builder()
                .newState(state.toBuilder()
                        .status(reachedPickup ? DriverStatus.WAITING_FOR_PASSENGER : DriverStatus.MOVING_TO_PICKUP)
                        .currentPosition(nextPosition)
                        .build())
                .rideId(state.getActiveRideId())
                .reachedPickup(reachedPickup)
                .build();
    }

    public MovementUpdate toRepositioningUpdate(DriverGeoState state, boolean reachedTarget, GeoPoint nextPosition) {
        return MovementUpdate.builder()
                .newState(state.toBuilder()
                        .status(reachedTarget ? DriverStatus.IDLE : DriverStatus.REPOSITIONING)
                        .available(reachedTarget)
                        .targetPosition(reachedTarget ? null : state.getTargetPosition())
                        .currentPosition(nextPosition)
                        .build())
                .becameAvailable(reachedTarget)
                .build();
    }

    public MovementUpdate toScenarioBiasedCruisingUpdate(DriverGeoState state, GeoPoint nextPosition) {
        return MovementUpdate.builder()
                .newState(state.toBuilder()
                        .currentPosition(nextPosition)
                        .build())
                .build();
    }

    public MovementUpdate toTripProgressUpdate(DriverGeoState state, boolean reachedDropoff, GeoPoint nextPosition) {
        return MovementUpdate.builder()
                .newState(state.toBuilder()
                        .status(reachedDropoff ? DriverStatus.IDLE : DriverStatus.ON_TRIP)
                        .available(reachedDropoff)
                        .activeRideId(reachedDropoff ? null : state.getActiveRideId())
                        .pickupPosition(reachedDropoff ? null : state.getPickupPosition())
                        .dropoffPosition(reachedDropoff ? null : state.getDropoffPosition())
                        .currentPosition(nextPosition)
                        .build())
                .rideId(state.getActiveRideId())
                .reachedDestination(reachedDropoff)
                .becameAvailable(reachedDropoff)
                .build();
    }

    public MovementUpdate toUnchangedUpdate(DriverGeoState state) {
        return MovementUpdate.builder()
                .newState(state)
                .build();
    }

    public GeoPoint toGeoPoint(double latitude, double longitude) {
        return GeoPoint.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
