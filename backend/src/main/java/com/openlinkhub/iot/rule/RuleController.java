package com.openlinkhub.iot.rule;

import com.openlinkhub.iot.common.ApiResponse;
import com.openlinkhub.iot.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService service;

    public RuleController(RuleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Rule>> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String deviceKey,
                                              @RequestParam(required = false) String metric,
                                              @RequestParam(required = false) Boolean enabled,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.findPage(keyword, deviceKey, metric, enabled, page, size));
    }

    @PostMapping
    public ApiResponse<Rule> create(@Valid @RequestBody RuleRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Rule> update(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Rule> enable(@PathVariable Long id) {
        return ApiResponse.ok(service.enable(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Rule> disable(@PathVariable Long id) {
        return ApiResponse.ok(service.disable(id));
    }
}
