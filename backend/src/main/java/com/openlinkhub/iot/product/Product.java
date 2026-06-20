package com.openlinkhub.iot.product;

import java.time.OffsetDateTime;

public record Product(
        Long id,
        String name,
        String code,
        String category,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
