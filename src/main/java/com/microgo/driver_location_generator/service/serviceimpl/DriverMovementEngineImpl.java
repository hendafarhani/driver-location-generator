package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.function.UnaryOperator;

@Service
@RequiredArgsConstructor
public class DriverMovementEngineImpl implements DriverMovementEngine {

    private static final double CENTRAL_LONDON_LATITUDE = 51.5074d;
    private static final double CENTRAL_LONDON_LONGITUDE = -0.1278d;
    private static final double LATITUDE_KILOMETERS_PER_DEGREE = 55.0d;
    private static final double LONGITUDE_KILOMETERS_PER_DEGREE = 35.0d;
    private static final int POSITION_BUCKET_COUNT = 1_000;
    private static final int LONGITUDE_HASH_DIVISOR = 31;

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
        mutate(command.getDriverId(), state -> DriverMovementEngineMapper.toTripCommandState(
                state,
                command,
                point(command.getDestinationLatitude(), command.getDestinationLongitude())));
    }

    @Override
    public void stopDriver(StopDriverCommand command) {
        mutate(command.getDriverId(), DriverMovementEngineMapper::toStoppedDriverState);
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

    private static boolean isOffline(DriverGeoState existingState) {
        return existingState.getStatus() == DriverStatus.OFFLINE;
    }

    private static boolean hasChangedZone(DriverGeoState existingState, DriverGeoState advancedState) {
        return existingState.getCurrentZone() != advancedState.getCurrentZone();
    }

    private void persistAndPublish(MovementUpdate update) {
        DriverGeoState state = update.getNewState();
        persistAndPublishLocation(state);
        if (update.isZoneChanged()) {
            publisher.publishZoneEntered(state);
        }
        if (update.isReachedPickup()) {
            publisher.publishReachedPickup(state.getDriverId(), update.getRideId());
        }
        if (update.isReachedDestination()) {
            publisher.publishReachedDestination(state.getDriverId(), update.getRideId());
        }
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
        if (config == null) {
            return defaultCentralLondonPosition();
        }

        long hash = Math.abs((long) driverId.hashCode());
        double latOffset = getLatOffset(hash, config);
        double lonOffset = getLonOffset(hash, config);
        return point(getLatitude(config, latOffset), getLongitude(config, lonOffset));
    }

    private DriverLocationGeneratorProperties.ScenarioConfig scenarioConfig(LondonScenario scenario) {
        if (scenario == null) {
            return null;
        }
        DriverLocationGeneratorProperties.ScenarioKey scenarioKey =
                DriverLocationGeneratorProperties.ScenarioKey.valueOf(scenario.name());
        return properties.getScenarios().get(scenarioKey);
    }

    private GeoPoint defaultCentralLondonPosition() {
        return point(CENTRAL_LONDON_LATITUDE, CENTRAL_LONDON_LONGITUDE);
    }

    private static double getLatOffset(long hash, DriverLocationGeneratorProperties.ScenarioConfig config) {
        return centeredBucket(hash) * config.getSpreadKm() / LATITUDE_KILOMETERS_PER_DEGREE;
    }

    private static double getLonOffset(long hash, DriverLocationGeneratorProperties.ScenarioConfig config) {
        return centeredBucket(hash / LONGITUDE_HASH_DIVISOR)
                * config.getSpreadKm()
                / LONGITUDE_KILOMETERS_PER_DEGREE;
    }

    private static double centeredBucket(long hash) {
        return (hash % POSITION_BUCKET_COUNT) / (double) POSITION_BUCKET_COUNT - 0.5d;
    }

    private static double getLongitude(DriverLocationGeneratorProperties.ScenarioConfig config, double lonOffset) {
        return config.getCenterLongitude() + lonOffset;
    }

    private static double getLatitude(DriverLocationGeneratorProperties.ScenarioConfig config, double latOffset) {
        return config.getCenterLatitude() + latOffset;
    }

    private static GeoPoint point(double latitude, double longitude) {
        return DriverMovementEngineMapper.toGeoPoint(latitude, longitude);
    }
}
