package com.fleetwise.api.telematics.repository;

import com.fleetwise.api.telematics.entity.TelematicsDevice;
import com.fleetwise.api.telematics.entity.TelematicsProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelematicsDeviceRepository
        extends JpaRepository<TelematicsDevice, UUID> {

    List<TelematicsDevice> findByVehicleIdAndActiveTrue(UUID vehicleId);

    Optional<TelematicsDevice> findByProviderAndExternalDeviceIdAndActiveTrue(
            TelematicsProvider provider,
            String externalDeviceId
    );

    Optional<TelematicsDevice> findByProviderAndSerialNumberAndActiveTrue(
            TelematicsProvider provider,
            String serialNumber
    );

    Optional<TelematicsDevice> findByProviderAndVinAndActiveTrue(
            TelematicsProvider provider,
            String vin
    );
}