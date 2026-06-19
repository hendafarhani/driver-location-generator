package com.microgo.driver_location_generator.service;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.domain.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.domain.LondonScenario;
import com.microgo.driver_location_generator.domain.LondonZone;
import com.microgo.driver_location_generator.entity.DriverProfileEntity;
import com.microgo.driver_location_generator.kafka.model.DriverGeneratedEvent;
import com.microgo.driver_location_generator.kafka.model.MoveToPickupCommand;
import com.microgo.driver_location_generator.kafka.model.RepositionDriverCommand;
import com.microgo.driver_location_generator.kafka.model.StartTripCommand;
import com.microgo.driver_location_generator.kafka.model.StopDriverCommand;
import com.microgo.driver_location_generator.service.serviceimpl.DriverMovementEngineImpl;
import com.microgo.driver_location_generator.service.serviceimpl.LondonDistanceMatrixImpl;
import com.microgo.driver_location_generator.service.serviceimpl.LondonTrafficModelImpl;
import com.microgo.driver_location_generator.service.serviceimpl.LondonZoneServiceImpl;
import com.microgo.driver_location_generator.service.serviceimpl.MovementStrategyResolverImpl;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.IdleMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.PickupMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.RepositioningMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.ScenarioMovementStrategy;
import com.microgo.driver_location_generator.service.strategy.strategyimpl.TripMovementStrategy;
import com.microgo.driver_location_generator.store.RedisGeoDriverStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverMovementEngineTest {

    private DriverLocationGeneratorProperties properties;
    private InMemoryStateStore stateStore;
    private RecordingPublisher publisher;
    private DriverMovementEngine engine;

    @BeforeEach
    void setUp() {
        properties = new DriverLocationGeneratorProperties();
        properties.setArrivalThresholdMeters(35);
        DriverLocationGeneratorProperties.ScenarioConfig concert =
                new DriverLocationGeneratorProperties.ScenarioConfig();
        concert.setCenterLatitude(51.5560);
        concert.setCenterLongitude(-0.2796);
        concert.setSpreadKm(3);
        concert.setDemandBiasLatitude(51.5560);
        concert.setDemandBiasLongitude(-0.2796);
        concert.setTrafficMultiplier(0.9);
        properties.setScenarios(new EnumMap<>(DriverLocationGeneratorProperties.ScenarioKey.class));
        properties.getScenarios().put(DriverLocationGeneratorProperties.ScenarioKey.CONCERT_RAIN, concert);

        stateStore = new InMemoryStateStore();
        publisher = new RecordingPublisher();
        LondonDistanceMatrix distanceMatrix = new LondonDistanceMatrixImpl();
        MovementStrategyResolver resolver = new MovementStrategyResolverImpl(List.of(
                new RepositioningMovementStrategy(properties, distanceMatrix),
                new PickupMovementStrategy(properties, distanceMatrix),
                new TripMovementStrategy(properties, distanceMatrix),
                new ScenarioMovementStrategy(properties, distanceMatrix),
                new IdleMovementStrategy(distanceMatrix)));
        engine = new DriverMovementEngineImpl(
                properties,
                stateStore,
                new LondonZoneServiceImpl(),
                new LondonTrafficModelImpl(properties),
                resolver,
                publisher,
                new StubIdentityService());
    }

    @Test
    void registersDriverWithScenarioPositionAndPublishesIt() {
        engine.registerDriver(DriverGeneratedEvent.builder()
                .driverId("driver-1")
                .driverDisplayId("DRV-1")
                .scenario(LondonScenario.CONCERT_RAIN)
                .build());

        DriverGeoState state = stateStore.required("driver-1");
        assertThat(state.getDriverDisplayId()).isEqualTo("DRV-1");
        assertThat(state.getStatus()).isEqualTo(DriverStatus.IDLE);
        assertThat(state.isAvailable()).isTrue();
        assertThat(state.getCurrentZone()).isNotNull();
        assertThat(state.getCurrentPosition().getLatitude()).isBetween(51.52, 51.59);
        assertThat(publisher.locationUpdates).containsExactly(state);
    }

    @Test
    void commandTransitionsUpdateTargetsRideAndStepSize() {
        stateStore.save(idleState());

        engine.repositionDriver(RepositionDriverCommand.builder()
                .driverId("driver-1")
                .targetLatitude(51.51)
                .targetLongitude(-0.12)
                .build());
        DriverGeoState repositioning = stateStore.required("driver-1");
        assertThat(repositioning.getStatus()).isEqualTo(DriverStatus.REPOSITIONING);
        assertThat(repositioning.getTargetPosition()).isEqualTo(point(51.51, -0.12));
        assertThat(repositioning.getPlannedStepMeters())
                .isEqualTo(properties.getRepositionStepMeters() * 0.9 * 0.72);

        engine.moveToPickup(MoveToPickupCommand.builder()
                .driverId("driver-1")
                .rideId("ride-1")
                .pickupLatitude(51.52)
                .pickupLongitude(-0.13)
                .build());
        DriverGeoState pickup = stateStore.required("driver-1");
        assertThat(pickup.getStatus()).isEqualTo(DriverStatus.MOVING_TO_PICKUP);
        assertThat(pickup.getActiveRideId()).isEqualTo("ride-1");
        assertThat(pickup.getPickupPosition()).isEqualTo(point(51.52, -0.13));

        engine.startTrip(StartTripCommand.builder()
                .driverId("driver-1")
                .rideId("ride-1")
                .destinationLatitude(51.53)
                .destinationLongitude(-0.14)
                .build());
        DriverGeoState trip = stateStore.required("driver-1");
        assertThat(trip.getStatus()).isEqualTo(DriverStatus.ON_TRIP);
        assertThat(trip.getDropoffPosition()).isEqualTo(point(51.53, -0.14));

        engine.stopDriver(StopDriverCommand.builder().driverId("driver-1").build());
        DriverGeoState stopped = stateStore.required("driver-1");
        assertThat(stopped.getStatus()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(stopped.isAvailable()).isFalse();
        assertThat(stopped.getActiveRideId()).isNull();
        assertThat(stopped.getTargetPosition()).isNull();
        assertThat(stopped.getPickupPosition()).isNull();
        assertThat(stopped.getDropoffPosition()).isNull();
    }

    @Test
    void rejectsCommandsForUnknownDriver() {
        assertThatThrownBy(() -> engine.stopDriver(
                StopDriverCommand.builder().driverId("missing").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown driver missing");
    }

    @Test
    void advancesPickupArrivalAndPublishesReachedPickup() {
        stateStore.save(idleState().toBuilder()
                .status(DriverStatus.MOVING_TO_PICKUP)
                .activeRideId("ride-1")
                .pickupPosition(point(51.5561, -0.2796))
                .build());

        engine.advanceAllDrivers();

        DriverGeoState state = stateStore.required("driver-1");
        assertThat(state.getStatus()).isEqualTo(DriverStatus.WAITING_FOR_PASSENGER);
        assertThat(state.getTickSequence()).isEqualTo(1);
        assertThat(publisher.reachedPickups).containsExactly("driver-1:ride-1");
    }

    @Test
    void advancesTripArrivalAndPublishesAvailability() {
        stateStore.save(idleState().toBuilder()
                .status(DriverStatus.ON_TRIP)
                .available(false)
                .activeRideId("ride-1")
                .dropoffPosition(point(51.5561, -0.2796))
                .build());

        engine.advanceAllDrivers();

        DriverGeoState state = stateStore.required("driver-1");
        assertThat(state.getStatus()).isEqualTo(DriverStatus.IDLE);
        assertThat(state.isAvailable()).isTrue();
        assertThat(publisher.reachedDestinations).containsExactly("driver-1:ride-1");
        assertThat(publisher.availableDrivers).containsExactly("driver-1");
    }

    @Test
    void keepsOfflineDriverUnchangedButStillPublishesSnapshot() {
        DriverGeoState offline = idleState().toBuilder()
                .status(DriverStatus.OFFLINE)
                .available(false)
                .tickSequence(4)
                .build();
        stateStore.save(offline);

        engine.advanceAllDrivers();

        assertThat(stateStore.required("driver-1")).isEqualTo(offline);
        assertThat(publisher.locationUpdates).containsExactly(offline);
    }

    @Test
    void handlesMinimumIntegerHashWithoutLeavingScenarioSpread() {
        String minimumHashDriverId = "polygenelubricants";
        assertThat(minimumHashDriverId.hashCode()).isEqualTo(Integer.MIN_VALUE);

        engine.registerDriver(DriverGeneratedEvent.builder()
                .driverId(minimumHashDriverId)
                .scenario(LondonScenario.CONCERT_RAIN)
                .build());

        GeoPoint position = stateStore.required(minimumHashDriverId).getCurrentPosition();
        assertThat(position.getLatitude()).isBetween(51.52, 51.59);
        assertThat(position.getLongitude()).isBetween(-0.33, -0.23);
    }

    private static DriverGeoState idleState() {
        return DriverGeoState.builder()
                .driverId("driver-1")
                .driverDisplayId("DRV-1")
                .scenario(LondonScenario.CONCERT_RAIN)
                .status(DriverStatus.IDLE)
                .currentPosition(point(51.5560, -0.2796))
                .currentZone(LondonZone.WEMBLEY_EVENT_ZONE)
                .available(true)
                .build();
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }

    private static final class InMemoryStateStore extends RedisGeoDriverStateStore {
        private final Map<String, DriverGeoState> states = new HashMap<>();

        private InMemoryStateStore() {
            super(null, null, null);
        }

        @Override
        public void save(DriverGeoState state) {
            states.put(state.getDriverId(), state);
        }

        @Override
        public Optional<DriverGeoState> findByDriverId(String driverId) {
            return Optional.ofNullable(states.get(driverId));
        }

        @Override
        public Set<DriverGeoState> findAll() {
            return Set.copyOf(states.values());
        }

        private DriverGeoState required(String driverId) {
            return findByDriverId(driverId).orElseThrow();
        }
    }

    private static final class RecordingPublisher implements DriverLocationPublisher {
        private final List<DriverGeoState> locationUpdates = new ArrayList<>();
        private final List<String> reachedPickups = new ArrayList<>();
        private final List<String> reachedDestinations = new ArrayList<>();
        private final List<String> availableDrivers = new ArrayList<>();

        @Override
        public void publishLocationUpdated(DriverGeoState state) {
            locationUpdates.add(state);
        }

        @Override
        public void publishZoneEntered(DriverGeoState state) {
        }

        @Override
        public void publishReachedPickup(String driverId, String rideId) {
            reachedPickups.add(driverId + ":" + rideId);
        }

        @Override
        public void publishReachedDestination(String driverId, String rideId) {
            reachedDestinations.add(driverId + ":" + rideId);
        }

        @Override
        public void publishBecameAvailable(String driverId) {
            availableDrivers.add(driverId);
        }
    }

    private static final class StubIdentityService implements DriverIdentityService {
        @Override
        public DriverProfileEntity ensureProfile(String driverIdentifier, String requestedDisplayId) {
            return DriverProfileEntity.builder()
                    .driverIdentifier(driverIdentifier)
                    .driverDisplayId(requestedDisplayId == null ? "DRV-" + driverIdentifier : requestedDisplayId)
                    .build();
        }
    }
}
