package com.openlinkhub.iot.telemetry;

import com.openlinkhub.iot.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final TelemetryService telemetryService;

    public IngestController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/http/{deviceKey}/telemetry")
    public ApiResponse<IngestResult> ingest(@PathVariable String deviceKey,
                                            @RequestHeader(value = "X-Device-Secret", required = false) String secret,
                                            @Valid @RequestBody TelemetryIngestRequest request) {
        return ApiResponse.ok(telemetryService.ingest(deviceKey, secret, request));
    }
}
