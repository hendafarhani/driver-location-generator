package com.microgo.driver_location_generator.kafka.model;

import com.microgo.driver_location_generator.domain.LondonScenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverGeneratedEvent {
    private String driverId;
    private String driverDisplayId;
    private LondonScenario scenario;
}
