package com.openlinkhub.iot.product;

import java.time.OffsetDateTime;

public record Product(
        Long id,
        String name,
        String code,
        String category,
        String protocolType,
        String protocolConfig,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
