package com.microgo.driver_location_generator.kafka.handler;

import com.microgo.driver_location_generator.kafka.model.DriverAcceptedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverRefusedEvent;
import com.microgo.driver_location_generator.kafka.model.RideCancelledEvent;
import com.microgo.driver_location_generator.service.DriverMovementEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Translates ride decisions made elsewhere (simulation driver decision engine, dispatch)
 * into generator movement: accept drives the driver to pickup, cancel frees a busy driver,
 * and refuse leaves an uncommitted driver available.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideDecisionListener {

    private final DriverMovementEngine movementEngine;

    @KafkaListener(
            id = "${kafka.listeners.driver-accepted.id}",
            topics = "${kafka.topics.driver-accepted}",
            groupId = "${kafka.consumers.driver-accepted.group-id}",
            containerFactory = "driverAcceptedListenerFactory"
    )
    public void onDriverAccepted(DriverAcceptedEvent event) {
        if (event == null || !StringUtils.hasText(event.getDriverId())) {
            log.warn("Ignoring driver-accepted event without a driver id: {}", event);
            return;
        }
        movementEngine.acceptRide(event);
    }

    @KafkaListener(
            id = "${kafka.listeners.driver-refused.id}",
            topics = "${kafka.topics.driver-refused}",
            groupId = "${kafka.consumers.driver-refused.group-id}",
            containerFactory = "driverRefusedListenerFactory"
    )
    public void onDriverRefused(DriverRefusedEvent event) {
        if (event == null || !StringUtils.hasText(event.getDriverId())) {
            log.warn("Ignoring driver-refused event without a driver id: {}", event);
            return;
        }
        movementEngine.declineRide(event);
    }

    @KafkaListener(
            id = "${kafka.listeners.ride-cancelled.id}",
            topics = "${kafka.topics.ride-cancelled}",
            groupId = "${kafka.consumers.ride-cancelled.group-id}",
            containerFactory = "rideCancelledListenerFactory"
    )
    public void onRideCancelled(RideCancelledEvent event) {
        if (event == null || !StringUtils.hasText(event.getDriverId())) {
            log.warn("Ignoring ride-cancelled event without a driver id: {}", event);
            return;
        }
        movementEngine.cancelRide(event);
    }
}
