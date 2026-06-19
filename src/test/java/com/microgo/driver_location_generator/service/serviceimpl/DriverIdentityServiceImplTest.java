package com.microgo.driver_location_generator.service.serviceimpl;

import com.microgo.driver_location_generator.entity.DriverProfileEntity;
import com.microgo.driver_location_generator.repository.DriverProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverIdentityServiceImplTest {

    private DriverProfileRepository repository;
    private DriverIdentityServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(DriverProfileRepository.class);
        service = new DriverIdentityServiceImpl(repository);
    }

    @Test
    void createsProfileWithRequestedDisplayId() {
        when(repository.findByDriverIdentifier("driver-1")).thenReturn(Optional.empty());
        when(repository.save(any(DriverProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DriverProfileEntity profile = service.ensureProfile("driver-1", "DRV-LONDON-1");

        assertThat(profile.getDriverIdentifier()).isEqualTo("driver-1");
        assertThat(profile.getDriverDisplayId()).isEqualTo("DRV-LONDON-1");
        verify(repository).save(profile);
    }

    @Test
    void createsNormalizedFallbackDisplayId() {
        when(repository.findByDriverIdentifier("driver central/42")).thenReturn(Optional.empty());
        when(repository.save(any(DriverProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DriverProfileEntity profile = service.ensureProfile("driver central/42", " ");

        assertThat(profile.getDriverDisplayId()).isEqualTo("DRV-DRIVER-CENTRAL-42");
    }

    @Test
    void returnsExistingProfileWithoutOverwritingDisplayId() {
        DriverProfileEntity existing = DriverProfileEntity.builder()
                .driverIdentifier("driver-1")
                .driverDisplayId("DRV-EXISTING")
                .build();
        when(repository.findByDriverIdentifier("driver-1")).thenReturn(Optional.of(existing));

        DriverProfileEntity profile = service.ensureProfile("driver-1", "DRV-NEW");

        assertThat(profile).isSameAs(existing);
        assertThat(profile.getDriverDisplayId()).isEqualTo("DRV-EXISTING");
        verify(repository, never()).save(any());
    }

    @Test
    void fillsMissingDisplayIdOnExistingProfile() {
        DriverProfileEntity existing = DriverProfileEntity.builder()
                .driverIdentifier("driver-1")
                .driverDisplayId(null)
                .build();
        when(repository.findByDriverIdentifier("driver-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        DriverProfileEntity profile = service.ensureProfile("driver-1", null);

        assertThat(profile.getDriverDisplayId()).isEqualTo("DRV-DRIVER-1");
        verify(repository).save(existing);
    }
}
