package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.kafka.model.DriverAcceptedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverRefusedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.RideCancelledEvent;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;

public interface DriverMovementEngine {

    void registerDriver(DriverGeneratedEvent event);

    void repositionDriver(RepositionDriverCommand command);

    void moveToPickup(MoveToPickupCommand command);

    void startTrip(StartTripCommand command);

    void stopDriver(StopDriverCommand command);

    /**
     * Reaction to a driver accepting a ride offer: send the driver toward the pickup.
     * The trip itself still begins on an explicit {@link StartTripCommand}.
     */
    void acceptRide(DriverAcceptedEvent event);

    /**
     * Reaction to a driver refusing a ride offer: the driver was never committed, so it
     * simply remains available for the next offer.
     */
    void declineRide(DriverRefusedEvent event);

    /**
     * Reaction to a ride being cancelled: if the driver is currently serving that ride,
     * halt it and return it to the available pool.
     */
    void cancelRide(RideCancelledEvent event);

    void advanceAllDrivers();
}
