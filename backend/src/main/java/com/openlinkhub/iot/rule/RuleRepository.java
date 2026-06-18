package com.openlinkhub.iot.rule;

import com.openlinkhub.iot.common.PageResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class RuleRepository {

    private final JdbcClient jdbc;

    public RuleRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Rule> findAll() {
        return jdbc.sql(baseSelect() + " ORDER BY id DESC")
                .query(this::mapRule)
                .list();
    }

    public PageResult<Rule> findPage(String keyword, String deviceKey, String metric, Boolean enabled, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        String keywordFilter = keyword == null || keyword.isBlank()
                ? ""
                : " AND (name ILIKE :keyword OR description ILIKE :keyword)\n";
        String deviceFilter = deviceKey == null || deviceKey.isBlank() ? "" : " AND device_key = :deviceKey\n";
        String metricFilter = metric == null || metric.isBlank() ? "" : " AND metric = :metric\n";
        String enabledFilter = enabled == null ? "" : " AND enabled = :enabled\n";

        JdbcClient.StatementSpec countSpec = jdbc.sql("""
                SELECT COUNT(*)
                FROM olh_rule
                WHERE TRUE
                """ + keywordFilter + deviceFilter + metricFilter + enabledFilter);
        JdbcClient.StatementSpec dataSpec = jdbc.sql(baseSelect() + """
                WHERE TRUE
                """ + keywordFilter + deviceFilter + metricFilter + enabledFilter + """
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
                """)
                .param("limit", safeSize)
                .param("offset", offset);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            countSpec = countSpec.param("keyword", like);
            dataSpec = dataSpec.param("keyword", like);
        }
        if (deviceKey != null && !deviceKey.isBlank()) {
            countSpec = countSpec.param("deviceKey", deviceKey);
            dataSpec = dataSpec.param("deviceKey", deviceKey);
        }
        if (metric != null && !metric.isBlank()) {
            countSpec = countSpec.param("metric", metric);
            dataSpec = dataSpec.param("metric", metric);
        }
        if (enabled != null) {
            countSpec = countSpec.param("enabled", enabled);
            dataSpec = dataSpec.param("enabled", enabled);
        }

        return new PageResult<>(
                dataSpec.query(this::mapRule).list(),
                countSpec.query(Long.class).single(),
                safePage,
                safeSize
        );
    }

    public List<Rule> findEnabledFor(String deviceKey, String metric) {
        return jdbc.sql(baseSelect() + """
                WHERE enabled = TRUE
                  AND metric = :metric
                  AND (device_key IS NULL OR device_key = '' OR device_key = :deviceKey)
                ORDER BY id
                """)
                .param("deviceKey", deviceKey)
                .param("metric", metric)
                .query(this::mapRule)
                .list();
    }

    public Optional<Rule> findById(Long id) {
        return jdbc.sql(baseSelect() + " WHERE id = :id")
                .param("id", id)
                .query(this::mapRule)
                .optional();
    }

    public Rule create(RuleRequest request) {
        Long id = jdbc.sql("""
                INSERT INTO olh_rule (name, device_key, metric, operator, threshold, severity, enabled, description)
                VALUES (:name, :deviceKey, :metric, :operator, :threshold,
                        COALESCE(:severity, 'warning'), COALESCE(:enabled, TRUE), :description)
                RETURNING id
                """)
                .param("name", request.name())
                .param("deviceKey", request.deviceKey())
                .param("metric", request.metric())
                .param("operator", request.operator())
                .param("threshold", request.threshold())
                .param("severity", request.severity())
                .param("enabled", request.enabled())
                .param("description", request.description())
                .query(Long.class)
                .single();
        return findById(id).orElseThrow();
    }

    public Rule update(Long id, RuleRequest request) {
        jdbc.sql("""
                UPDATE olh_rule
                SET name = :name,
                    device_key = :deviceKey,
                    metric = :metric,
                    operator = :operator,
                    threshold = :threshold,
                    severity = COALESCE(:severity, 'warning'),
                    enabled = COALESCE(:enabled, TRUE),
                    description = :description,
                    updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("name", request.name())
                .param("deviceKey", request.deviceKey())
                .param("metric", request.metric())
                .param("operator", request.operator())
                .param("threshold", request.threshold())
                .param("severity", request.severity())
                .param("enabled", request.enabled())
                .param("description", request.description())
                .update();
        return findById(id).orElseThrow();
    }

    public Rule setEnabled(Long id, boolean enabled) {
        jdbc.sql("UPDATE olh_rule SET enabled = :enabled, updated_at = NOW() WHERE id = :id")
                .param("id", id)
                .param("enabled", enabled)
                .update();
        return findById(id).orElseThrow();
    }

    private String baseSelect() {
        return """
                SELECT id, name, device_key, metric, operator, threshold, severity, enabled,
                       description, created_at, updated_at
                FROM olh_rule
                """;
    }

    private Rule mapRule(ResultSet rs, int rowNum) throws SQLException {
        return new Rule(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("device_key"),
                rs.getString("metric"),
                rs.getString("operator"),
                rs.getDouble("threshold"),
                rs.getString("severity"),
                rs.getBoolean("enabled"),
                rs.getString("description"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
