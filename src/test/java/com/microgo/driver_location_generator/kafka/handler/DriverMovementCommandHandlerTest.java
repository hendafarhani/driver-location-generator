package com.microgo.driver_location_generator.kafka.handler;

import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;
import com.microgo.driver_location_generator.service.DriverMovementEngine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DriverMovementCommandHandlerTest {

    private final DriverMovementEngine movementEngine = mock(DriverMovementEngine.class);
    private final DriverMovementCommandHandler handler = new DriverMovementCommandHandler(movementEngine);

    @Test
    void delegatesEveryCommandToMovementEngine() {
        DriverGeneratedEvent generatedEvent = DriverGeneratedEvent.builder().driverId("driver-1").build();
        RepositionDriverCommand repositionCommand = RepositionDriverCommand.builder().driverId("driver-1").build();
        MoveToPickupCommand pickupCommand = MoveToPickupCommand.builder().driverId("driver-1").build();
        StartTripCommand tripCommand = StartTripCommand.builder().driverId("driver-1").build();
        StopDriverCommand stopCommand = StopDriverCommand.builder().driverId("driver-1").build();

        handler.onDriverGenerated(generatedEvent);
        handler.onRepositionDriver(repositionCommand);
        handler.onMoveToPickup(pickupCommand);
        handler.onStartTrip(tripCommand);
        handler.onStopDriver(stopCommand);

        verify(movementEngine).registerDriver(generatedEvent);
        verify(movementEngine).repositionDriver(repositionCommand);
        verify(movementEngine).moveToPickup(pickupCommand);
        verify(movementEngine).startTrip(tripCommand);
        verify(movementEngine).stopDriver(stopCommand);
    }
}
