package com.microgo.driver_location_generator.kafka.handler;

import com.microgo.driver_location_generator.kafka.model.DriverAcceptedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverRefusedEvent;
import com.microgo.driver_location_generator.kafka.model.RideCancelledEvent;
import com.microgo.driver_location_generator.service.DriverMovementEngine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RideDecisionListenerTest {

    private final DriverMovementEngine movementEngine = mock(DriverMovementEngine.class);
    private final RideDecisionListener listener = new RideDecisionListener(movementEngine);

    @Test
    void delegatesEachDecisionToMovementEngine() {
        DriverAcceptedEvent accepted = DriverAcceptedEvent.builder().driverId("driver-1").rideId("ride-1").build();
        DriverRefusedEvent refused = DriverRefusedEvent.builder().driverId("driver-1").rideId("ride-1").build();
        RideCancelledEvent cancelled = RideCancelledEvent.builder().driverId("driver-1").rideId("ride-1").build();

        listener.onDriverAccepted(accepted);
        listener.onDriverRefused(refused);
        listener.onRideCancelled(cancelled);

        verify(movementEngine).acceptRide(accepted);
        verify(movementEngine).declineRide(refused);
        verify(movementEngine).cancelRide(cancelled);
    }

    @Test
    void ignoresEventsWithoutDriverId() {
        listener.onDriverAccepted(DriverAcceptedEvent.builder().rideId("ride-1").build());
        listener.onDriverRefused(new DriverRefusedEvent());
        listener.onRideCancelled(null);

        verifyNoInteractions(movementEngine);
    }
}
