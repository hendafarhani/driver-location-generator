package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.businessrule.DriverSeedBusinessRules;
import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.kafka.model.DriverAcceptedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverRefusedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.RideCancelledEvent;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;
import com.microgo.driver_location_generator.mapper.DriverMovementEngineMapper;
import com.microgo.driver_location_generator.service.DriverIdentityService;
import com.microgo.driver_location_generator.kafka.publisher.DriverLocationPublisher;
import com.microgo.driver_location_generator.service.DriverMovementEngine;
import com.microgo.driver_location_generator.service.LondonTrafficModel;
import com.microgo.driver_location_generator.service.LondonZoneService;
import com.microgo.driver_location_generator.service.MovementStrategyResolver;
import com.microgo.driver_location_generator.service.MovementUpdate;
import com.microgo.driver_location_generator.store.RedisGeoDriverStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.UnaryOperator;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverMovementEngineImpl implements DriverMovementEngine {

    private static final double CENTRAL_LONDON_LATITUDE = 51.5074d;
    private static final double CENTRAL_LONDON_LONGITUDE = -0.1278d;

    private final DriverLocationGeneratorProperties properties;
    private final RedisGeoDriverStateStore stateStore;
    private final LondonZoneService zoneService;
    private final LondonTrafficModel trafficModel;
    private final MovementStrategyResolver strategyResolver;
    private final DriverLocationPublisher publisher;
    private final DriverIdentityService driverIdentityService;

    @Override
    public void registerDriver(DriverGeneratedEvent event) {
        var profile = driverIdentityService.ensureProfile(event.getDriverId(), event.getDriverDisplayId());
        GeoPoint initialPosition = initialPosition(profile.getDriverIdentifier(), event.getScenario());
        persistAndPublishLocation(DriverMovementEngineMapper.toRegisteredDriverState(
                profile,
                event.getScenario(),
                properties.getIdleStepMeters(),
                initialPosition,
                zoneService.resolveZone(initialPosition),
                Instant.now()));
    }

    @Override
    public void repositionDriver(RepositionDriverCommand command) {
        mutate(command.getDriverId(), state -> DriverMovementEngineMapper.toRepositioningCommandState(
                state,
                command,
                point(command.getTargetLatitude(), command.getTargetLongitude())));
    }

    @Override
    public void moveToPickup(MoveToPickupCommand command) {
        mutate(command.getDriverId(), state -> DriverMovementEngineMapper.toPickupCommandState(
                state,
                command,
                point(command.getPickupLatitude(), command.getPickupLongitude())));
    }

    @Override
    public void startTrip(StartTripCommand command) {
        DriverGeoState state = stateStore.findByDriverId(command.getDriverId()).orElse(null);
        if (state == null) {
            log.debug("Ignoring start-trip for unknown driver {}", command.getDriverId());
            return;
        }
        if (isServingRide(state) && isDifferentRide(state, command)) {
            log.debug("Ignoring start-trip {} for driver {} already serving ride {}",
                    command.getRideId(), command.getDriverId(), state.getActiveRideId());
            return;
        }
        mutate(command.getDriverId(), current -> DriverMovementEngineMapper.toTripCommandState(
                current,
                command,
                point(command.getDestinationLatitude(), command.getDestinationLongitude())));
    }

    @Override
    public void stopDriver(StopDriverCommand command) {
        mutate(command.getDriverId(), DriverMovementEngineMapper::toStoppedDriverState);
    }

    @Override
    public void acceptRide(DriverAcceptedEvent event) {
        stateStore.findByDriverId(event.getDriverId()).ifPresentOrElse(state -> {
            if (isServingRide(state)) {
                log.debug("Ignoring acceptance of ride {} for driver {} already serving ride {}",
                        event.getRideId(), event.getDriverId(), state.getActiveRideId());
                return;
            }
            moveToPickup(MoveToPickupCommand.builder()
                    .driverId(event.getDriverId())
                    .rideId(event.getRideId())
                    .pickupLatitude(event.getPickupLatitude())
                    .pickupLongitude(event.getPickupLongitude())
                    .build());
        }, () -> log.debug("Ignoring acceptance for unknown driver {}", event.getDriverId()));
    }

    @Override
    public void declineRide(DriverRefusedEvent event) {
        // A refused offer never committed the driver, so there is nothing to free; it stays available.
        log.debug("Driver {} refused ride {}; remaining available", event.getDriverId(), event.getRideId());
    }

    @Override
    public void cancelRide(RideCancelledEvent event) {
        stateStore.findByDriverId(event.getDriverId()).ifPresentOrElse(state -> {
            if (!isServingThisRide(state, event.getRideId())) {
                log.debug("Ignoring cancellation of ride {} for driver {} (active ride is {})",
                        event.getRideId(), event.getDriverId(), state.getActiveRideId());
                return;
            }
            mutate(event.getDriverId(), DriverMovementEngineMapper::toFreedDriverState);
            publisher.publishBecameAvailable(event.getDriverId());
        }, () -> log.debug("Ignoring cancellation for unknown driver {}", event.getDriverId()));
    }

    @Override
    public void advanceAllDrivers() {
        stateStore.findAll().stream()
                .sorted(Comparator.comparing(DriverGeoState::getDriverId))
                .map(this::advance)
                .forEach(this::persistAndPublish);
    }

    private MovementUpdate advance(DriverGeoState existingState) {
        if (isOffline(existingState)) {
            return DriverMovementEngineMapper.toUnchangedMovementUpdate(existingState);
        }

        double adjustedStep = trafficModel.stepMeters(existingState);
        DriverGeoState stepAwareState = DriverMovementEngineMapper.toStepAwareState(existingState, adjustedStep);
        MovementUpdate rawUpdate = strategyResolver.resolve(stepAwareState).move(stepAwareState);
        DriverGeoState advancedState = finalizeAdvance(existingState, rawUpdate, adjustedStep, Instant.now());

        return DriverMovementEngineMapper.toAdvancedMovementUpdate(
                advancedState,
                hasChangedZone(existingState, advancedState),
                rawUpdate);
    }

    private DriverGeoState finalizeAdvance(
            DriverGeoState existingState,
            MovementUpdate rawUpdate,
            double adjustedStep,
            Instant advancedAt) {
        long nextTickSequence = existingState.getTickSequence() + 1;
        if (hasNoMovementStep(adjustedStep)) {
            return DriverMovementEngineMapper.toNoMovementAdvancedState(
                    existingState,
                    nextTickSequence,
                    advancedAt);
        }

        DriverGeoState movedState = rawUpdate.getNewState();
        return DriverMovementEngineMapper.toAdvancedDriverState(
                existingState,
                movedState,
                nextTickSequence,
                advancedAt,
                zoneService.resolveZone(movedState.getCurrentPosition()));
    }

    private static boolean hasNoMovementStep(double adjustedStep) {
        return adjustedStep == 0.0d;
    }

    private static boolean isServingRide(DriverGeoState state) {
        return state.getActiveRideId() != null;
    }

    private static boolean isDifferentRide(DriverGeoState state, StartTripCommand command) {
        return !Objects.equals(state.getActiveRideId(), command.getRideId());
    }

    private static boolean isServingThisRide(DriverGeoState state, String rideId) {
        return state.getActiveRideId() != null && Objects.equals(state.getActiveRideId(), rideId);
    }

    private static boolean isOffline(DriverGeoState existingState) {
        return existingState.getStatus() == DriverStatus.OFFLINE;
    }

    private static boolean hasChangedZone(DriverGeoState existingState, DriverGeoState advancedState) {
        return existingState.getCurrentZone() != advancedState.getCurrentZone();
    }

    private void persistAndPublish(MovementUpdate update) {
        DriverGeoState state = update.getNewState();
        persistAndPublishLocation(state);
        publishZoneChangeEvent(update, state);
        publishPickupReachedEvent(update, state);
        publishDestinationReachedEvent(update, state);
        publishAvailabilityChangeEvent(update, state);
    }

    private void publishZoneChangeEvent(MovementUpdate update, DriverGeoState state) {
        if (update.isZoneChanged()) {
            publisher.publishZoneEntered(state);
        }
    }

    private void publishPickupReachedEvent(MovementUpdate update, DriverGeoState state) {
        if (update.isReachedPickup()) {
            publisher.publishReachedPickup(state.getDriverId(), update.getRideId());
        }
    }

    private void publishDestinationReachedEvent(MovementUpdate update, DriverGeoState state) {
        if (update.isReachedDestination()) {
            publisher.publishReachedDestination(state.getDriverId(), update.getRideId());
        }
    }

    private void publishAvailabilityChangeEvent(MovementUpdate update, DriverGeoState state) {
        if (update.isBecameAvailable()) {
            publisher.publishBecameAvailable(state.getDriverId());
        }
    }

    private void persistAndPublishLocation(DriverGeoState state) {
        stateStore.save(state);
        publisher.publishLocationUpdated(state);
    }

    private void mutate(String driverId, UnaryOperator<DriverGeoState> mutator) {
        DriverGeoState currentState = findDriver(driverId);
        DriverGeoState commandState = mutator.apply(currentState);
        DriverGeoState updatedState = DriverMovementEngineMapper.toMutatedDriverState(
                commandState,
                Instant.now(),
                zoneService.resolveZone(commandState.getCurrentPosition()),
                trafficModel.stepMeters(commandState));
        persistAndPublishLocation(updatedState);
    }

    private DriverGeoState findDriver(String driverId) {
        return stateStore.findByDriverId(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown driver " + driverId));
    }

    private GeoPoint initialPosition(String driverId, LondonScenario scenario) {
        DriverLocationGeneratorProperties.ScenarioConfig config = scenarioConfig(scenario);
        return DriverSeedBusinessRules.initialPosition(
                driverId,
                config,
                CENTRAL_LONDON_LATITUDE,
                CENTRAL_LONDON_LONGITUDE);
    }

    private DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig(LondonScenario scenario) {
        if (isNoScenario(scenario)) {
            return null;
        }
        DriverLocationGeneratorProperties.ScenarioKey scenarioKey =
                DriverLocationGeneratorProperties.ScenarioKey.valueOf(scenario.name());
        return properties.getScenarios().get(scenarioKey);
    }

    private static boolean isNoScenario(LondonScenario scenario) {
        return scenario == null;
    }


    private static GeoPoint point(double latitude, double longitude) {
        return DriverMovementEngineMapper.toGeoPoint(latitude, longitude);
    }
}
