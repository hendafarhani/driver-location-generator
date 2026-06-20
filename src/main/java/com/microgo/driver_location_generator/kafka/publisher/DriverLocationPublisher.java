package com.microgo.driver_location_generator.kafka.publisher;

import com.microgo.driver_location_generator.domain.DriverGeoState;

public interface DriverLocationPublisher {

    void publishLocationUpdated(DriverGeoState state);

    void publishZoneEntered(DriverGeoState state);

    void publishReachedPickup(String driverId, String rideId);

    void publishReachedDestination(String driverId, String rideId);

    void publishBecameAvailable(String driverId);
}
