package com.openlinkhub.iot.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, OffsetDateTime.now());
    }
}
