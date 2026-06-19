package com.microgo.driver_location_generator.repository;

import com.microgo.driver_location_generator.entity.DriverProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfileEntity, Long> {

    Optional<DriverProfileEntity> findByDriverIdentifier(String driverIdentifier);
}
