package com.microgo.driver_location_generator.scheduler;

import com.microgo.driver_location_generator.service.DriverMovementEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DriverMovementScheduler {

    private final DriverMovementEngine movementEngine;

    @Scheduled(fixedDelayString = "${driver-location-generator.movement-interval-ms:5000}")
    public void tick() {
        movementEngine.advanceAllDrivers();
    }
}
