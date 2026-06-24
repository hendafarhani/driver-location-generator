package com.microgo.driver_location_generator.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideCancelledEvent {
    private String driverId;
    private String rideId;
}
