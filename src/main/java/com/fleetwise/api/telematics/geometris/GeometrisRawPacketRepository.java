package com.fleetwise.api.telematics.geometris;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeometrisRawPacketRepository
        extends JpaRepository<GeometrisRawPacket, UUID> {

    List<GeometrisRawPacket> findTop50ByOrderByReceivedAtDesc();

    List<GeometrisRawPacket> findTop50ByParsedSuccessfullyFalseOrderByReceivedAtDesc();
}