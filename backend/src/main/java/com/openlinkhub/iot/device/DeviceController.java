package com.openlinkhub.iot.device;

import com.openlinkhub.iot.common.ApiResponse;
import com.openlinkhub.iot.common.PageResult;
import com.openlinkhub.iot.telemetry.TelemetryLatest;
import com.openlinkhub.iot.telemetry.TelemetryPoint;
import com.openlinkhub.iot.telemetry.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final TelemetryService telemetryService;

    public DeviceController(DeviceService deviceService, TelemetryService telemetryService) {
        this.deviceService = deviceService;
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public ApiResponse<PageResult<Device>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long productId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(deviceService.findPage(keyword, productId, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Device> detail(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.findById(id));
    }

    @PostMapping
    public ApiResponse<Device> create(@Valid @RequestBody DeviceRequest request) {
        return ApiResponse.ok(deviceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Device> update(@PathVariable Long id, @Valid @RequestBody DeviceRequest request) {
        return ApiResponse.ok(deviceService.update(id, request));
    }

    @GetMapping("/{id}/latest")
    public ApiResponse<List<TelemetryLatest>> latest(@PathVariable Long id) {
        return ApiResponse.ok(telemetryService.latestByDevice(id));
    }

    @GetMapping("/{id}/telemetry")
    public ApiResponse<List<TelemetryPoint>> telemetry(@PathVariable Long id,
                                                       @RequestParam(required = false) String metric,
                                                       @RequestParam(required = false) OffsetDateTime start,
                                                       @RequestParam(required = false) OffsetDateTime end,
                                                       @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(telemetryService.historyByDevice(id, metric, start, end, limit));
    }
}
