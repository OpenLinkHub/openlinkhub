package com.openlinkhub.iot.product;

import jakarta.validation.constraints.NotBlank;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String code,
        String category,
        String description,
        String thingModel
) {
}
