# Driver Location Generator

Spring Boot microservice responsible for simulated driver movement in microGo.

It consumes driver lifecycle and movement commands from Kafka, stores live driver
positions in Redis GEO, persists driver profiles in PostgreSQL, and publishes
location and trip milestone events.

## Main responsibilities

- Register generated drivers with deterministic scenario positions.
- Advance idle, repositioning, pickup, and trip movement.
- Apply London scenario, traffic, and zone behavior.
- Store live positions under the shared Redis GEO key `vehicle_location`.
- Publish location, zone-entry, pickup, destination, and availability events.

## Verification

```bash
mvn -Djacoco.skip=true test
```

The JaCoCo skip is recommended on the current JDK 25 development environment.
The service targets Java 21.
