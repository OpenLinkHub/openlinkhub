package com.openlinkhub.iot.telemetry;

import com.openlinkhub.iot.alarm.Alarm;

import java.util.List;

public record IngestResult(
        String deviceKey,
        int acceptedMetrics,
        List<Alarm> alarms
) {
}
