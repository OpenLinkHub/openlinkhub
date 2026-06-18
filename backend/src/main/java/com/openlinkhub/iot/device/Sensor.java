package com.openlinkhub.iot.device;

import java.time.OffsetDateTime;

public record Sensor(
        Long id,
        Long deviceId,
        String name,
        String sensorKey,
        String sensorType,
        String unit,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
