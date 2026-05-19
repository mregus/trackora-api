package com.fleetwise.api.maintenance;

import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.maintenance.dto.CreateMaintenanceRequest;
import com.fleetwise.api.maintenance.dto.UpdateMaintenanceRequest;
import com.fleetwise.api.maintenance.entity.*;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.maintenance.service.MaintenanceService;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceServiceTest {

    private final MaintenanceRepository repo = mock(MaintenanceRepository.class);
    private final VehicleRepository vehicleRepo = mock(VehicleRepository.class);
    private final AlertRepository alertRepo = mock(AlertRepository.class);
    private final MaintenanceService service = new MaintenanceService(repo, vehicleRepo, alertRepo);

    @Test
    void create_ShouldPersistAndReturnResponse() {
        UUID vId = UUID.randomUUID(), uId = UUID.randomUUID();
        when(vehicleRepo.findByIdAndFleetOwnerId(vId, uId))
                .thenReturn(Optional.of(new Vehicle()));
        when(repo.save(any(Maintenance.class))).thenAnswer(i -> i.getArgument(0));

        var req = new CreateMaintenanceRequest(
                "Oil Change", "Full synthetic", LocalDate.now(), 123000,
                new BigDecimal("89.99"), "Shop", null
        );
        var resp = service.create(vId, uId, req);
        assertEquals("Oil Change", resp.serviceType());
    }

    @Test
    void update_ShouldApplyNewValues() {
        UUID id = UUID.randomUUID(), user = UUID.randomUUID();

        Maintenance m = new Maintenance();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        m.setVehicle(vehicle); // attach vehicle so toResponse() won't crash

        when(repo.findByIdAndVehicleFleetOwnerId(id, user)).thenReturn(Optional.of(m));

        var req = new UpdateMaintenanceRequest(
                "Brakes", "Pads", LocalDate.now(), 10,
                new BigDecimal("200"), "Vendor", null, MaintenanceStatus.COMPLETED
        );

        var resp = service.update(id, user, req);
        assertEquals("Brakes", resp.serviceType());
    }


    @Test
    void getRecord_NotFound_ShouldThrow() {
        when(repo.findByIdAndVehicleFleetOwnerId(any(), any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getRecord(UUID.randomUUID(), UUID.randomUUID()));
    }
}
