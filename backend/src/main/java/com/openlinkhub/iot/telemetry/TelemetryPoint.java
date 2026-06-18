package com.openlinkhub.iot.telemetry;

import java.time.OffsetDateTime;

public record TelemetryPoint(
        OffsetDateTime time,
        Long deviceId,
        Long productId,
        String metric,
        Double numericValue,
        String textValue,
        String valueType,
        String quality,
        String rawValue
) {
}
