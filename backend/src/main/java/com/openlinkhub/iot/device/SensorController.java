package com.openlinkhub.iot.device;

import com.openlinkhub.iot.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SensorController {

    private final SensorService service;

    public SensorController(SensorService service) {
        this.service = service;
    }

    @GetMapping("/devices/{deviceId}/sensors")
    public ApiResponse<List<Sensor>> list(@PathVariable Long deviceId) {
        return ApiResponse.ok(service.findByDeviceId(deviceId));
    }

    @PostMapping("/devices/{deviceId}/sensors")
    public ApiResponse<Sensor> create(@PathVariable Long deviceId, @Valid @RequestBody SensorRequest request) {
        return ApiResponse.ok(service.create(deviceId, request));
    }

    @PutMapping("/sensors/{id}")
    public ApiResponse<Sensor> update(@PathVariable Long id, @Valid @RequestBody SensorRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/sensors/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
