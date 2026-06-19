package com.microgo.driver_location_generator.kafka.handler;

import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;
import com.microgo.driver_location_generator.service.DriverMovementEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverMovementCommandHandler {

    private final DriverMovementEngine movementEngine;

    @KafkaListener(
            id = "${kafka.listeners.driver-generated.id}",
            topics = "${kafka.topics.driver-generated}",
            groupId = "${kafka.consumers.driver-generated.group-id}",
            containerFactory = "driverGeneratedListenerFactory"
    )
    public void onDriverGenerated(DriverGeneratedEvent event) {
        log.info("Generating initial state for driver {} in scenario {}", event.getDriverId(), event.getScenario());
        movementEngine.registerDriver(event);
    }

    @KafkaListener(
            id = "${kafka.listeners.reposition-driver-command.id}",
            topics = "${kafka.topics.reposition-driver-command}",
            groupId = "${kafka.consumers.reposition-driver-command.group-id}",
            containerFactory = "repositionDriverCommandListenerFactory"
    )
    public void onRepositionDriver(RepositionDriverCommand command) {
        movementEngine.repositionDriver(command);
    }

    @KafkaListener(
            id = "${kafka.listeners.move-to-pickup-command.id}",
            topics = "${kafka.topics.move-to-pickup-command}",
            groupId = "${kafka.consumers.move-to-pickup-command.group-id}",
            containerFactory = "moveToPickupCommandListenerFactory"
    )
    public void onMoveToPickup(MoveToPickupCommand command) {
        movementEngine.moveToPickup(command);
    }

    @KafkaListener(
            id = "${kafka.listeners.start-trip-command.id}",
            topics = "${kafka.topics.start-trip-command}",
            groupId = "${kafka.consumers.start-trip-command.group-id}",
            containerFactory = "startTripCommandListenerFactory"
    )
    public void onStartTrip(StartTripCommand command) {
        movementEngine.startTrip(command);
    }

    @KafkaListener(
            id = "${kafka.listeners.stop-driver-command.id}",
            topics = "${kafka.topics.stop-driver-command}",
            groupId = "${kafka.consumers.stop-driver-command.group-id}",
            containerFactory = "stopDriverCommandListenerFactory"
    )
    public void onStopDriver(StopDriverCommand command) {
        movementEngine.stopDriver(command);
    }
}
