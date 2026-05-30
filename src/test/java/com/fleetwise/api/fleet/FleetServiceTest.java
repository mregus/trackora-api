package com.fleetwise.api.fleet;

import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.dto.CreateFleetRequest;
import com.fleetwise.api.fleet.dto.UpdateFleetRequest;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.fleet.service.FleetService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FleetServiceTest {

    private final FleetRepository fleetRepo = mock(FleetRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final FleetService service = new FleetService(fleetRepo, userRepo, activityLogService);

    @Test
    void createFleet_ShouldSaveFleet() {
        UUID userId = UUID.randomUUID();
        var user = new User(); user.setId(userId);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(fleetRepo.save(any(Fleet.class))).thenAnswer(i -> {
            Fleet f = i.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        var resp = service.createFleet(userId, new CreateFleetRequest("My Fleet"));
        assertEquals("My Fleet", resp.name());
        verify(fleetRepo).save(any(Fleet.class));
    }

    @Test
    void getFleet_InvalidOwner_ShouldThrow() {
        when(fleetRepo.findByIdAndOwnerId(any(), any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.getFleet(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void updateFleet_ShouldChangeName() {
        UUID fleetId = UUID.randomUUID(), userId = UUID.randomUUID();
        Fleet fleet = new Fleet();
        fleet.setName("Old");

        // mock user and assign to fleet
        User owner = new User();
        owner.setId(userId);
        fleet.setOwner(owner);

        when(fleetRepo.findByIdAndOwnerId(fleetId, userId)).thenReturn(Optional.of(fleet));

        var resp = service.updateFleet(fleetId, userId, new UpdateFleetRequest("NewName"));

        assertEquals("NewName", resp.name());
        assertEquals(userId, resp.ownerUserId());
    }

}
