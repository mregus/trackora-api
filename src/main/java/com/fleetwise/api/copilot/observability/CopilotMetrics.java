package com.fleetwise.api.copilot.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class CopilotMetrics {

    private final MeterRegistry registry;

    public CopilotMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void requestStarted() {
        registry.counter(
                "trackora.copilot.requests"
        ).increment();
    }

    public void requestCompleted(boolean aiGenerated) {
        registry.counter(
                "trackora.copilot.responses",
                "mode", aiGenerated ? "ai" : "fallback"
        ).increment();
    }

    public void requestFailed(String reason) {
        registry.counter(
                "trackora.copilot.failures",
                "reason", normalize(reason)
        ).increment();
    }

    public void fallbackUsed(String reason) {
        registry.counter(
                "trackora.copilot.fallbacks",
                "reason", normalize(reason)
        ).increment();
    }

    public <T> T recordAiCall(Supplier<T> supplier) {
        return Timer.builder("trackora.copilot.ai.duration")
                .description("Fleet Copilot OpenAI request duration")
                .publishPercentileHistogram()
                .register(registry)
                .record(supplier);
    }

    public <T> T recordToolCall(
            String toolName,
            Supplier<T> supplier
    ) {
        return Timer.builder("trackora.copilot.tool.duration")
                .description("Fleet Copilot tool execution duration")
                .tag("tool", toolName)
                .publishPercentileHistogram()
                .register(registry)
                .record(supplier);
    }

    public void toolSucceeded(String toolName) {
        registry.counter(
                "trackora.copilot.tool.executions",
                "tool", toolName,
                "outcome", "success"
        ).increment();
    }

    public void toolFailed(
            String toolName,
            String reason
    ) {
        registry.counter(
                "trackora.copilot.tool.executions",
                "tool", toolName,
                "outcome", "failure",
                "reason", normalize(reason)
        ).increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_]+", "_");
    }

    public void toolRound(int callCount) {
        registry.counter(
                "trackora.copilot.tool.rounds",
                "call_count", bucketCallCount(callCount)
        ).increment();
    }

    private String bucketCallCount(int count) {
        if (count <= 1) {
            return "1";
        }

        if (count <= 3) {
            return "2_3";
        }

        return "4_plus";
    }
}