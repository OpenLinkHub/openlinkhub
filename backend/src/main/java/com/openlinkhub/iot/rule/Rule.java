package com.openlinkhub.iot.rule;

import java.time.OffsetDateTime;

public record Rule(
        Long id,
        String name,
        String deviceKey,
        String metric,
        String operator,
        Double threshold,
        String severity,
        Boolean enabled,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
