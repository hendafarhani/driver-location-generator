package com.microgo.driver_location_generator.scheduler;

import com.microgo.driver_location_generator.service.DriverMovementEngine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DriverMovementSchedulerTest {

    @Test
    void advancesAllDriversOnTick() {
        DriverMovementEngine movementEngine = mock(DriverMovementEngine.class);
        DriverMovementScheduler scheduler = new DriverMovementScheduler(movementEngine);

        scheduler.tick();

        verify(movementEngine).advanceAllDrivers();
    }
}
