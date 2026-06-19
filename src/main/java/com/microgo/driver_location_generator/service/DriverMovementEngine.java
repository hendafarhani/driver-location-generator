package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;

public interface DriverMovementEngine {

    void registerDriver(DriverGeneratedEvent event);

    void repositionDriver(RepositionDriverCommand command);

    void moveToPickup(MoveToPickupCommand command);

    void startTrip(StartTripCommand command);

    void stopDriver(StopDriverCommand command);

    void advanceAllDrivers();
}
