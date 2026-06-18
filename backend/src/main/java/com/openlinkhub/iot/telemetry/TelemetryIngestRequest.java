package com.openlinkhub.iot.telemetry;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Map;

public record TelemetryIngestRequest(
        OffsetDateTime timestamp,
        @NotNull Map<String, Object> values,
        String quality
) {
}
