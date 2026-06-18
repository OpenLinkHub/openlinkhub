package com.openlinkhub.iot.alarm;

import com.openlinkhub.iot.common.ApiResponse;
import com.openlinkhub.iot.common.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService service;

    public AlarmController(AlarmService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Alarm>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String severity,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.findPage(keyword, status, severity, page, size));
    }

    @PostMapping("/{id}/ack")
    public ApiResponse<Alarm> acknowledge(@PathVariable Long id) {
        return ApiResponse.ok(service.acknowledge(id));
    }
}
