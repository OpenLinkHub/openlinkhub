package com.openlinkhub.iot.device;

import jakarta.validation.constraints.NotBlank;

public record SensorRequest(
        @NotBlank String name,
        @NotBlank String sensorKey,
        String sensorType,
        String unit,
        String description
) {
}
