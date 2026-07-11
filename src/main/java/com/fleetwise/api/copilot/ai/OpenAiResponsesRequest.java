package com.fleetwise.api.copilot.ai;

import java.util.List;

public record OpenAiResponsesRequest(
        String model,
        String instructions,
        String input
) {
}