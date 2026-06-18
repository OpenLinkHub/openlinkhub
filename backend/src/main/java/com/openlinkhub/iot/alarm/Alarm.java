package com.openlinkhub.iot.alarm;

import java.time.OffsetDateTime;

public record Alarm(
        Long id,
        Long ruleId,
        Long deviceId,
        String deviceKey,
        String metric,
        Double value,
        Double threshold,
        String operator,
        String severity,
        String status,
        String message,
        OffsetDateTime occurredAt,
        OffsetDateTime acknowledgedAt,
        OffsetDateTime createdAt
) {
}
