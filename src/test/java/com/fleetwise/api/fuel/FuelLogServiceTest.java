package com.fleetwise.api.fuel;

import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fuel.dto.CreateFuelLogRequest;
import com.fleetwise.api.fuel.dto.UpdateFuelLogRequest;
import com.fleetwise.api.fuel.entity.FuelLog;
import com.fleetwise.api.fuel.repository.FuelLogRepository;
import com.fleetwise.api.fuel.service.FuelLogService;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FuelLogServiceTest {

    private final FuelLogRepository repo = mock(FuelLogRepository.class);
    private final VehicleRepository vehicleRepo = mock(VehicleRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final FuelLogService service = new FuelLogService(repo, vehicleRepo, activityLogService, userRepo);

    @Test
    void create_ShouldSaveFuelLog() {
        UUID vId = UUID.randomUUID(), owner = UUID.randomUUID();
        when(vehicleRepo.findByIdAndFleetOwnerId(vId, owner))
                .thenReturn(Optional.of(new Vehicle()));
        when(repo.save(any(FuelLog.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepo.findById(owner)).thenReturn(Optional.of(new User()));

        var req = new CreateFuelLogRequest(LocalDate.now(), 1000,
                new BigDecimal("10"), new BigDecimal("35.0"), "note");
        var resp = service.create(vId, owner, req);
        assertEquals(new BigDecimal("10.000"), resp.gallons().setScale(3));
    }

    @Test
    void update_ShouldModifyValues() {
        UUID id = UUID.randomUUID(), user = UUID.randomUUID();

        FuelLog log = new FuelLog();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        log.setVehicle(vehicle); // attach vehicle so toResponse() won't crash

        when(repo.findByIdAndVehicleFleetOwnerId(id, user))
                .thenReturn(Optional.of(log));

        var req = new UpdateFuelLogRequest(
                LocalDate.now(),
                1000,
                new BigDecimal("12"),
                new BigDecimal("38.4"),
                "updated"
        );

        var res = service.update(id, user, req);
        assertEquals("updated", res.notes());
    }

    @Test
    void get_NotFound_ShouldThrow() {
        when(repo.findByIdAndVehicleFleetOwnerId(any(), any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.get(UUID.randomUUID(), UUID.randomUUID()));
    }
}
