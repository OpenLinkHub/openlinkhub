package com.openlinkhub.iot.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.iot.alarm.Alarm;
import com.openlinkhub.iot.alarm.AlarmRepository;
import com.openlinkhub.iot.common.ApiException;
import com.openlinkhub.iot.device.Device;
import com.openlinkhub.iot.device.DeviceRepository;
import com.openlinkhub.iot.rule.Rule;
import com.openlinkhub.iot.rule.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryService {

    private final DeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;
    private final RuleRepository ruleRepository;
    private final AlarmRepository alarmRepository;
    private final ObjectMapper objectMapper;

    public TelemetryService(DeviceRepository deviceRepository,
                            TelemetryRepository telemetryRepository,
                            RuleRepository ruleRepository,
                            AlarmRepository alarmRepository,
                            ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
        this.ruleRepository = ruleRepository;
        this.alarmRepository = alarmRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestResult ingest(String deviceKey, String secret, TelemetryIngestRequest request) {
        Device device = deviceRepository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
        if (secret != null && !secret.isBlank() && !secret.equals(device.secret())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid device secret");
        }

        OffsetDateTime time = request.timestamp() == null ? OffsetDateTime.now() : request.timestamp();
        String quality = request.quality() == null || request.quality().isBlank() ? "good" : request.quality();
        List<Alarm> alarms = new ArrayList<>();

        for (Map.Entry<String, Object> entry : request.values().entrySet()) {
            String metric = entry.getKey();
            Object value = entry.getValue();
            ValueSnapshot snapshot = ValueSnapshot.from(value, objectMapper);

            telemetryRepository.insert(device.id(), device.productId(), time, metric,
                    snapshot.numericValue(), snapshot.textValue(), snapshot.valueType(), quality, snapshot.rawValue());
            telemetryRepository.upsertLatest(device.id(), device.productId(), time, metric,
                    snapshot.numericValue(), snapshot.textValue(), snapshot.valueType(), quality, snapshot.rawValue());

            if (snapshot.numericValue() != null) {
                for (Rule rule : ruleRepository.findEnabledFor(device.deviceKey(), metric)) {
                    if (matches(rule, snapshot.numericValue())) {
                        alarms.add(alarmRepository.create(rule, device.id(), device.deviceKey(), snapshot.numericValue(), time));
                    }
                }
            }
        }

        deviceRepository.markSeen(device.id());
        return new IngestResult(device.deviceKey(), request.values().size(), alarms);
    }

    public List<TelemetryLatest> latestByDevice(Long deviceId) {
        return telemetryRepository.latestByDevice(deviceId);
    }

    public List<TelemetryPoint> historyByDevice(Long deviceId, String metric, OffsetDateTime start, OffsetDateTime end, int limit) {
        return telemetryRepository.historyByDevice(deviceId, metric, start, end, limit);
    }

    public List<TelemetryPoint> recent(int limit) {
        return telemetryRepository.recent(limit);
    }

    public long countToday() {
        return telemetryRepository.countToday();
    }

    private boolean matches(Rule rule, Double value) {
        return switch (rule.operator()) {
            case ">" -> value > rule.threshold();
            case ">=" -> value >= rule.threshold();
            case "<" -> value < rule.threshold();
            case "<=" -> value <= rule.threshold();
            case "=", "==" -> value.doubleValue() == rule.threshold().doubleValue();
            case "!=" -> value.doubleValue() != rule.threshold().doubleValue();
            default -> false;
        };
    }

    private record ValueSnapshot(Double numericValue, String textValue, String valueType, String rawValue) {
        static ValueSnapshot from(Object value, ObjectMapper objectMapper) {
            try {
                String raw = objectMapper.writeValueAsString(value);
                if (value instanceof Number number) {
                    return new ValueSnapshot(number.doubleValue(), null, "number", raw);
                }
                if (value instanceof Boolean bool) {
                    return new ValueSnapshot(bool ? 1.0 : 0.0, Boolean.toString(bool), "boolean", raw);
                }
                return new ValueSnapshot(null, value == null ? null : String.valueOf(value), "string", raw);
            } catch (JsonProcessingException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Telemetry value is not JSON serializable");
            }
        }
    }
}
