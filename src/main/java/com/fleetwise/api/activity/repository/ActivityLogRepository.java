package com.fleetwise.api.activity.repository;

import com.fleetwise.api.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findTop50ByFleetIdOrderByCreatedAtDesc(UUID fleetId);
}