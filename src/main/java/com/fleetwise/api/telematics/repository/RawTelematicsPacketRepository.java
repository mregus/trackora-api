package com.fleetwise.api.telematics.repository;

import com.fleetwise.api.telematics.entity.RawTelematicsPacket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface RawTelematicsPacketRepository
        extends JpaRepository<RawTelematicsPacket, UUID> {

    long countByReceivedAtGreaterThanEqual(Instant start);
}