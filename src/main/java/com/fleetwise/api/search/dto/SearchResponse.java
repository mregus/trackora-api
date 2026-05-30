package com.fleetwise.api.search.dto;

import java.util.List;

public record SearchResponse(
        List<VehicleSearchResult> vehicles,
        List<MaintenanceSearchResult> maintenance,
        List<AlertSearchResult> alerts
) {}