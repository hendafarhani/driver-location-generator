package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.kafka.configuration.KafkaTopicProperties;
import com.microgo.driver_location_generator.mapper.DriverLocationEventMapper;
import com.microgo.driver_location_generator.service.DriverLocationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DriverLocationPublisherImpl implements DriverLocationPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    @Override
    public void publishLocationUpdated(DriverGeoState state) {
        kafkaTemplate.send(
                topics.getDriverLocationUpdated(),
                state.getDriverId(),
                DriverLocationEventMapper.toDriverLocationUpdatedEvent(state));
    }

    @Override
    public void publishZoneEntered(DriverGeoState state) {
        kafkaTemplate.send(
                topics.getDriverEnteredZone(),
                state.getDriverId(),
                DriverLocationEventMapper.toDriverEnteredZoneEvent(state, Instant.now()));
    }

    @Override
    public void publishReachedPickup(String driverId, String rideId) {
        kafkaTemplate.send(
                topics.getDriverReachedPickup(),
                driverId,
                DriverLocationEventMapper.toDriverReachedPickupEvent(driverId, rideId, Instant.now()));
    }

    @Override
    public void publishReachedDestination(String driverId, String rideId) {
        kafkaTemplate.send(
                topics.getDriverReachedDestination(),
                driverId,
                DriverLocationEventMapper.toDriverReachedDestinationEvent(driverId, rideId, Instant.now()));
    }

    @Override
    public void publishBecameAvailable(String driverId) {
        kafkaTemplate.send(
                topics.getDriverBecameAvailable(),
                driverId,
                DriverLocationEventMapper.toDriverBecameAvailableEvent(driverId, Instant.now()));
    }
}
