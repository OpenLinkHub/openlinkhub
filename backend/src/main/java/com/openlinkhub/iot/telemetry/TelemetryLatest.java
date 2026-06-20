package com.openlinkhub.iot.telemetry;

import java.time.OffsetDateTime;

public record TelemetryLatest(
        Long deviceId,
        String metric,
        String sensorName,
        String unit,
        OffsetDateTime time,
        Double numericValue,
        String textValue,
        String valueType,
        String quality,
        String rawValue
) {
}
