package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonScenario;
import com.microgo.driver_location_generator.enums.LondonZone;
import com.microgo.driver_location_generator.kafka.configuration.KafkaTopicProperties;
import com.microgo.driver_location_generator.kafka.model.DriverBecameAvailableEvent;
import com.microgo.driver_location_generator.kafka.model.DriverEnteredZoneEvent;
import com.microgo.driver_location_generator.kafka.model.DriverLocationUpdatedEvent;
import com.microgo.driver_location_generator.kafka.model.DriverReachedDestinationEvent;
import com.microgo.driver_location_generator.kafka.model.DriverReachedPickupEvent;
import com.microgo.driver_location_generator.kafka.publisher.impl.DriverLocationPublisherImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DriverLocationPublisherImplTest {

    private KafkaTemplate<String, Object> kafkaTemplate;
    private DriverLocationPublisherImpl publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        KafkaTopicProperties topics = new KafkaTopicProperties();
        topics.setDriverLocationUpdated("driver-location-updated");
        topics.setDriverEnteredZone("driver-entered-zone");
        topics.setDriverReachedPickup("driver-reached-pickup");
        topics.setDriverReachedDestination("driver-reached-destination");
        topics.setDriverBecameAvailable("driver-became-available");
        publisher = new DriverLocationPublisherImpl(kafkaTemplate, topics);
    }

    @Test
    void publishesLocationUpdateWithStableDriverIdentifiers() {
        Instant updatedAt = Instant.parse("2026-06-19T12:00:00Z");
        DriverGeoState state = DriverGeoState.builder()
                .driverId("driver-1")
                .driverDisplayId("DRV-1")
                .scenario(LondonScenario.CONCERT_RAIN)
                .status(DriverStatus.CRUISING)
                .currentZone(LondonZone.CENTRAL_LONDON)
                .currentPosition(point(51.5074, -0.1278))
                .available(true)
                .tickSequence(7)
                .updatedAt(updatedAt)
                .build();
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        publisher.publishLocationUpdated(state);

        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("driver-location-updated"),
                org.mockito.ArgumentMatchers.eq("driver-1"),
                eventCaptor.capture());
        DriverLocationUpdatedEvent event = (DriverLocationUpdatedEvent) eventCaptor.getValue();
        assertThat(event.getDriverId()).isEqualTo("driver-1");
        assertThat(event.getDriverIdentifier()).isEqualTo("driver-1");
        assertThat(event.getProviderIdentifier()).isEqualTo("driver-1");
        assertThat(event.getDriverDisplayId()).isEqualTo("DRV-1");
        assertThat(event.getLatitude()).isEqualTo(51.5074);
        assertThat(event.getLongitude()).isEqualTo(-0.1278);
        assertThat(event.getTickSequence()).isEqualTo(7);
        assertThat(event.getOccurredAt()).isEqualTo(updatedAt);
    }

    @Test
    void publishesLifecycleEventsToTheirConfiguredTopics() {
        DriverGeoState state = DriverGeoState.builder()
                .driverId("driver-1")
                .currentZone(LondonZone.WEMBLEY_EVENT_ZONE)
                .build();
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        publisher.publishZoneEntered(state);
        publisher.publishReachedPickup("driver-1", "ride-1");
        publisher.publishReachedDestination("driver-1", "ride-1");
        publisher.publishBecameAvailable("driver-1");

        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("driver-entered-zone"),
                org.mockito.ArgumentMatchers.eq("driver-1"),
                eventCaptor.capture());
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("driver-reached-pickup"),
                org.mockito.ArgumentMatchers.eq("driver-1"),
                eventCaptor.capture());
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("driver-reached-destination"),
                org.mockito.ArgumentMatchers.eq("driver-1"),
                eventCaptor.capture());
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("driver-became-available"),
                org.mockito.ArgumentMatchers.eq("driver-1"),
                eventCaptor.capture());
        verify(kafkaTemplate, times(4)).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("driver-1"),
                org.mockito.ArgumentMatchers.any());

        assertThat(eventCaptor.getAllValues())
                .hasExactlyElementsOfTypes(
                        DriverEnteredZoneEvent.class,
                        DriverReachedPickupEvent.class,
                        DriverReachedDestinationEvent.class,
                        DriverBecameAvailableEvent.class);
        assertThat(((DriverReachedPickupEvent) eventCaptor.getAllValues().get(1)).getRideId()).isEqualTo("ride-1");
        assertThat(((DriverReachedDestinationEvent) eventCaptor.getAllValues().get(2)).getRideId()).isEqualTo("ride-1");
    }

    private static GeoPoint point(double latitude, double longitude) {
        return GeoPoint.builder().latitude(latitude).longitude(longitude).build();
    }
}
