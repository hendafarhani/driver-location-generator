package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.entity.DriverProfileEntity;
import com.microgo.driver_location_generator.mapper.DriverProfileMapper;
import com.microgo.driver_location_generator.repository.DriverProfileRepository;
import com.microgo.driver_location_generator.service.DriverIdentityService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverIdentityServiceImpl implements DriverIdentityService {

    private final DriverProfileRepository driverProfileRepository;

    @Override
    @Transactional
    public DriverProfileEntity ensureProfile(String driverIdentifier, String requestedDisplayId) {
        return driverProfileRepository.findByDriverIdentifier(driverIdentifier)
                .map(profile -> updateMissingDisplayId(profile, requestedDisplayId))
                .orElseGet(() -> driverProfileRepository.save(DriverProfileMapper.toDriverProfileEntity(
                        driverIdentifier,
                        resolveDisplayId(driverIdentifier, requestedDisplayId))));
    }

    private DriverProfileEntity updateMissingDisplayId(DriverProfileEntity profile, String requestedDisplayId) {
        if (hasMissingDriverDisplayId(profile)) {
            profile.setDriverDisplayId(resolveDisplayId(profile.getDriverIdentifier(), requestedDisplayId));
            return driverProfileRepository.save(profile);
        }
        return profile;
    }

    private static boolean hasMissingDriverDisplayId(DriverProfileEntity profile) {
        return profile.getDriverDisplayId() == null || profile.getDriverDisplayId().isBlank();
    }

    String resolveDisplayId(String driverIdentifier, String requestedDisplayId) {
        if (hasRequestedDisplayId(requestedDisplayId)) {
            return requestedDisplayId;
        }
        return buildFallbackDriverDisplayId(driverIdentifier);
    }

    private static @NonNull String buildFallbackDriverDisplayId(String driverIdentifier) {
        return "DRV-" + driverIdentifier.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
    }

    private static boolean hasRequestedDisplayId(String requestedDisplayId) {
        return requestedDisplayId != null && !requestedDisplayId.isBlank();
    }
}
