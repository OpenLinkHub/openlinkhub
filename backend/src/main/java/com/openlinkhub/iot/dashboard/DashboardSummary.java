package com.openlinkhub.iot.dashboard;

import com.openlinkhub.iot.alarm.Alarm;
import com.openlinkhub.iot.telemetry.TelemetryPoint;

import java.util.List;

public record DashboardSummary(
        long productCount,
        long deviceCount,
        long onlineDeviceCount,
        long todayTelemetryCount,
        long openAlarmCount,
        List<TelemetryPoint> recentTelemetry,
        List<Alarm> recentAlarms
) {
}
