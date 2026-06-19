package com.microgo.driver_location_generator.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartTripCommand {
    private String driverId;
    private String rideId;
    private double destinationLatitude;
    private double destinationLongitude;
}
