package com.microgo.driver_location_generator.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import com.microgo.driver_location_generator.domain.DriverGeoState;
import com.microgo.driver_location_generator.domain.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisGeoDriverStateStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DriverLocationGeneratorProperties properties;

    public void save(DriverGeoState state) {
        stringRedisTemplate.opsForGeo().add(
                properties.getRedis().getGeoKey(),
                new Point(state.getCurrentPosition().getLongitude(), state.getCurrentPosition().getLatitude()),
                state.getDriverId());
        stringRedisTemplate.opsForValue().set(stateKey(state.getDriverId()), serialize(state));
        stringRedisTemplate.opsForValue().set(zoneKey(state.getDriverId()), state.getCurrentZone().name());
    }

    public Optional<DriverGeoState> findByDriverId(String driverId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(stateKey(driverId)))
                .map(this::deserialize);
    }

    public Set<DriverGeoState> findAll() {
        Set<String> keys = stringRedisTemplate.keys(properties.getRedis().getStateKeyPrefix() + "*");
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }
        return keys.stream()
                .map(key -> stringRedisTemplate.opsForValue().get(key))
                .map(this::deserialize)
                .collect(Collectors.toSet());
    }

    public Optional<String> currentZone(String driverId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(zoneKey(driverId)));
    }

    public void delete(String driverId) {
        stringRedisTemplate.opsForGeo().remove(properties.getRedis().getGeoKey(), driverId);
        stringRedisTemplate.delete(List.of(stateKey(driverId), zoneKey(driverId)));
    }

    public GeoPoint readCurrentPosition(String driverId) {
        List<Point> points = stringRedisTemplate.opsForGeo().position(properties.getRedis().getGeoKey(), driverId);
        if (points == null || points.isEmpty() || points.getFirst() == null) {
            return null;
        }
        Point point = points.getFirst();
        return GeoPoint.builder()
                .latitude(point.getY())
                .longitude(point.getX())
                .build();
    }

    private String stateKey(String driverId) {
        return properties.getRedis().getStateKeyPrefix() + driverId;
    }

    private String zoneKey(String driverId) {
        return properties.getRedis().getZoneKeyPrefix() + driverId;
    }

    private String serialize(DriverGeoState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize driver state " + state.getDriverId(), exception);
        }
    }

    private DriverGeoState deserialize(String value) {
        try {
            return objectMapper.readValue(value, DriverGeoState.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize driver state", exception);
        }
    }
}
