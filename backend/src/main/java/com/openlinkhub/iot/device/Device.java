package com.openlinkhub.iot.device;

import java.time.OffsetDateTime;

public record Device(
        Long id,
        Long productId,
        String productName,
        String name,
        String deviceKey,
        String secret,
        String location,
        String status,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
