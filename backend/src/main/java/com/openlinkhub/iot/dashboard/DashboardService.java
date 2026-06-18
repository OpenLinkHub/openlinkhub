package com.openlinkhub.iot.dashboard;

import com.openlinkhub.iot.alarm.AlarmRepository;
import com.openlinkhub.iot.telemetry.TelemetryService;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final TelemetryService telemetryService;
    private final AlarmRepository alarmRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                            TelemetryService telemetryService,
                            AlarmRepository alarmRepository) {
        this.dashboardRepository = dashboardRepository;
        this.telemetryService = telemetryService;
        this.alarmRepository = alarmRepository;
    }

    public DashboardSummary summary() {
        return new DashboardSummary(
                dashboardRepository.productCount(),
                dashboardRepository.deviceCount(),
                dashboardRepository.onlineDeviceCount(),
                telemetryService.countToday(),
                dashboardRepository.openAlarmCount(),
                telemetryService.recent(12),
                alarmRepository.findAll(null).stream().limit(6).toList()
        );
    }
}
