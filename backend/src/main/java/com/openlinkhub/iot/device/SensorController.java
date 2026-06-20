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

    @GetMapping("/products/{productId}/sensors")
    public ApiResponse<List<Sensor>> list(@PathVariable Long productId) {
        return ApiResponse.ok(service.findByProductId(productId));
    }

    @PostMapping("/products/{productId}/sensors")
    public ApiResponse<Sensor> create(@PathVariable Long productId, @Valid @RequestBody SensorRequest request) {
        return ApiResponse.ok(service.create(productId, request));
    }

    @PutMapping("/sensor-definitions/{id}")
    public ApiResponse<Sensor> update(@PathVariable Long id, @Valid @RequestBody SensorRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/sensor-definitions/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
