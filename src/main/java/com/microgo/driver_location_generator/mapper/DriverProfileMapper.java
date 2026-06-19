package com.microgo.driver_location_generator.mapper;

import com.microgo.driver_location_generator.entity.DriverProfileEntity;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DriverProfileMapper {

    public DriverProfileEntity toDriverProfileEntity(String driverIdentifier, String driverDisplayId) {
        return DriverProfileEntity.builder()
                .driverIdentifier(driverIdentifier)
                .driverDisplayId(driverDisplayId)
                .build();
    }
}
