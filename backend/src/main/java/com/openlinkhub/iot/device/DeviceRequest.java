package com.openlinkhub.iot.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequest(
        @NotNull Long productId,
        @NotBlank String name,
        @NotBlank String deviceKey,
        String secret,
        String location
) {
}
