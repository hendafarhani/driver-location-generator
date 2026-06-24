package com.microgo.driver_location_generator.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.enums.DriverStatus;
import com.microgo.driver_location_generator.domain.GeoPoint;
import com.microgo.driver_location_generator.enums.LondonZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisGeoDriverStateStoreTest {

    private StringRedisTemplate redisTemplate;
    private GeoOperations<String, String> geoOperations;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private RedisGeoDriverStateStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        geoOperations = mock(GeoOperations.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        store = new RedisGeoDriverStateStore(
                redisTemplate,
                objectMapper,
                new DriverLocationGeneratorProperties());
    }

    @Test
    void savesGeoSerializedStateAndZone() throws Exception {
        DriverGeoState state = state();

        store.save(state);

        verify(geoOperations).add("vehicle_location", new Point(-0.1278, 51.5074), "driver-1");
        verify(valueOperations).multiSet(java.util.Map.of(
                "driver:geo-state:driver-1", objectMapper.writeValueAsString(state),
                "driver:zone:driver-1", "CENTRAL_LONDON"));
    }

    @Test
    void findsSingleDriverAndAllDrivers() throws Exception {
        DriverGeoState state = state();
        String serialized = objectMapper.writeValueAsString(state);
        when(valueOperations.get("driver:geo-state:driver-1")).thenReturn(serialized);
        when(redisTemplate.keys("driver:geo-state:*")).thenReturn(Set.of("driver:geo-state:driver-1"));
        when(valueOperations.get(anyString())).thenReturn(serialized);

        assertThat(store.findByDriverId("driver-1")).contains(state);
        assertThat(store.findAll()).containsExactly(state);
    }

    @Test
    void handlesEmptyStateAndMissingGeoPosition() {
        when(redisTemplate.keys("driver:geo-state:*")).thenReturn(null);
        when(geoOperations.position("vehicle_location", "driver-1")).thenReturn(List.of());

        assertThat(store.findByDriverId("driver-1")).isEmpty();
        assertThat(store.findAll()).isEmpty();
        assertThat(store.currentZone("driver-1")).isEmpty();
        assertThat(store.readCurrentPosition("driver-1")).isNull();
    }

    @Test
    void readsPositionAndDeletesEveryDriverKey() {
        when(geoOperations.position("vehicle_location", "driver-1"))
                .thenReturn(List.of(new Point(-0.1278, 51.5074)));

        GeoPoint position = store.readCurrentPosition("driver-1");
        store.delete("driver-1");

        assertThat(position.getLatitude()).isEqualTo(51.5074);
        assertThat(position.getLongitude()).isEqualTo(-0.1278);
        verify(geoOperations).remove("vehicle_location", "driver-1");
        verify(redisTemplate).delete(List.of(
                "driver:geo-state:driver-1",
                "driver:zone:driver-1"));
    }

    @Test
    void wrapsInvalidSerializedState() {
        when(valueOperations.get("driver:geo-state:driver-1")).thenReturn("{invalid");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> store.findByDriverId("driver-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to deserialize driver state");
    }

    private static DriverGeoState state() {
        return DriverGeoState.builder()
                .driverId("driver-1")
                .status(DriverStatus.IDLE)
                .currentPosition(GeoPoint.builder().latitude(51.5074).longitude(-0.1278).build())
                .currentZone(LondonZone.CENTRAL_LONDON)
                .available(true)
                .build();
    }
}
