package com.openlinkhub.iot.alarm;

import com.openlinkhub.iot.common.PageResult;
import com.openlinkhub.iot.rule.Rule;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AlarmRepository {

    private final JdbcClient jdbc;

    public AlarmRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Alarm> findAll(String status) {
        String where = status == null || status.isBlank() ? "" : " WHERE status = :status";
        JdbcClient.StatementSpec spec = jdbc.sql(baseSelect() + where + " ORDER BY occurred_at DESC, id DESC LIMIT 200");
        if (status != null && !status.isBlank()) {
            spec = spec.param("status", status);
        }
        return spec.query(this::mapAlarm).list();
    }

    public PageResult<Alarm> findPage(String keyword, String status, String severity, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        String keywordFilter = keyword == null || keyword.isBlank()
                ? ""
                : " AND (device_key ILIKE :keyword OR metric ILIKE :keyword OR message ILIKE :keyword)\n";
        String statusFilter = status == null || status.isBlank() ? "" : " AND status = :status\n";
        String severityFilter = severity == null || severity.isBlank() ? "" : " AND severity = :severity\n";

        JdbcClient.StatementSpec countSpec = jdbc.sql("""
                SELECT COUNT(*)
                FROM olh_alarm
                WHERE TRUE
                """ + keywordFilter + statusFilter + severityFilter);
        JdbcClient.StatementSpec dataSpec = jdbc.sql(baseSelect() + """
                WHERE TRUE
                """ + keywordFilter + statusFilter + severityFilter + """
                ORDER BY occurred_at DESC, id DESC
                LIMIT :limit OFFSET :offset
                """)
                .param("limit", safeSize)
                .param("offset", offset);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            countSpec = countSpec.param("keyword", like);
            dataSpec = dataSpec.param("keyword", like);
        }
        if (status != null && !status.isBlank()) {
            countSpec = countSpec.param("status", status);
            dataSpec = dataSpec.param("status", status);
        }
        if (severity != null && !severity.isBlank()) {
            countSpec = countSpec.param("severity", severity);
            dataSpec = dataSpec.param("severity", severity);
        }

        return new PageResult<>(
                dataSpec.query(this::mapAlarm).list(),
                countSpec.query(Long.class).single(),
                safePage,
                safeSize
        );
    }

    public Optional<Alarm> findById(Long id) {
        return jdbc.sql(baseSelect() + " WHERE id = :id")
                .param("id", id)
                .query(this::mapAlarm)
                .optional();
    }

    public Alarm create(Rule rule, Long deviceId, String deviceKey, Double value, OffsetDateTime occurredAt) {
        String message = "%s %s %s %s, actual value is %s".formatted(
                deviceKey, rule.metric(), rule.operator(), rule.threshold(), value);
        Long id = jdbc.sql("""
                INSERT INTO olh_alarm (rule_id, device_id, device_key, metric, value, threshold, operator,
                                       severity, message, occurred_at)
                VALUES (:ruleId, :deviceId, :deviceKey, :metric, :value, :threshold, :operator,
                        :severity, :message, :occurredAt)
                RETURNING id
                """)
                .param("ruleId", rule.id())
                .param("deviceId", deviceId)
                .param("deviceKey", deviceKey)
                .param("metric", rule.metric())
                .param("value", value)
                .param("threshold", rule.threshold())
                .param("operator", rule.operator())
                .param("severity", rule.severity())
                .param("message", message)
                .param("occurredAt", occurredAt)
                .query(Long.class)
                .single();
        return findById(id).orElseThrow();
    }

    public Alarm acknowledge(Long id) {
        jdbc.sql("""
                UPDATE olh_alarm
                SET status = 'acknowledged',
                    acknowledged_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .update();
        return findById(id).orElseThrow();
    }

    private String baseSelect() {
        return """
                SELECT id, rule_id, device_id, device_key, metric, value, threshold, operator,
                       severity, status, message, occurred_at, acknowledged_at, created_at
                FROM olh_alarm
                """;
    }

    private Alarm mapAlarm(ResultSet rs, int rowNum) throws SQLException {
        return new Alarm(
                rs.getLong("id"),
                rs.getObject("rule_id", Long.class),
                rs.getObject("device_id", Long.class),
                rs.getString("device_key"),
                rs.getString("metric"),
                rs.getObject("value", Double.class),
                rs.getObject("threshold", Double.class),
                rs.getString("operator"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("message"),
                rs.getObject("occurred_at", java.time.OffsetDateTime.class),
                rs.getObject("acknowledged_at", java.time.OffsetDateTime.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        );
    }
}
