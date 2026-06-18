package com.openlinkhub.iot.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleRequest(
        @NotBlank String name,
        String deviceKey,
        @NotBlank String metric,
        @NotBlank String operator,
        @NotNull Double threshold,
        String severity,
        Boolean enabled,
        String description
) {
}
