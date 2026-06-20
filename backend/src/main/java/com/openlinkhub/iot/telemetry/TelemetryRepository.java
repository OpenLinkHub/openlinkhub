package com.openlinkhub.iot.telemetry;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class TelemetryRepository {

    private final JdbcClient jdbc;

    public TelemetryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Long deviceId, Long productId, OffsetDateTime time, String metric,
                       Double numericValue, String textValue, String valueType, String quality, String rawValue) {
        jdbc.sql("""
                INSERT INTO olh_telemetry_data
                    (time, device_id, product_id, metric, numeric_value, text_value, value_type, quality, raw_value)
                VALUES
                    (:time, :deviceId, :productId, :metric, :numericValue, :textValue,
                     :valueType, :quality, CAST(:rawValue AS jsonb))
                """)
                .param("time", time)
                .param("deviceId", deviceId)
                .param("productId", productId)
                .param("metric", metric)
                .param("numericValue", numericValue)
                .param("textValue", textValue)
                .param("valueType", valueType)
                .param("quality", quality)
                .param("rawValue", rawValue)
                .update();
    }

    public void upsertLatest(Long deviceId, Long productId, OffsetDateTime time, String metric,
                             Double numericValue, String textValue, String valueType, String quality, String rawValue) {
        jdbc.sql("""
                INSERT INTO olh_telemetry_latest
                    (device_id, metric, product_id, time, numeric_value, text_value, value_type, quality, raw_value)
                VALUES
                    (:deviceId, :metric, :productId, :time, :numericValue, :textValue,
                     :valueType, :quality, CAST(:rawValue AS jsonb))
                ON CONFLICT (device_id, metric) DO UPDATE
                SET product_id = EXCLUDED.product_id,
                    time = EXCLUDED.time,
                    numeric_value = EXCLUDED.numeric_value,
                    text_value = EXCLUDED.text_value,
                    value_type = EXCLUDED.value_type,
                    quality = EXCLUDED.quality,
                    raw_value = EXCLUDED.raw_value,
                    updated_at = NOW()
                """)
                .param("deviceId", deviceId)
                .param("metric", metric)
                .param("productId", productId)
                .param("time", time)
                .param("numericValue", numericValue)
                .param("textValue", textValue)
                .param("valueType", valueType)
                .param("quality", quality)
                .param("rawValue", rawValue)
                .update();
    }

    public List<TelemetryLatest> latestByDevice(Long deviceId) {
        return jdbc.sql("""
                SELECT device_id, metric, time, numeric_value, text_value, value_type, quality, raw_value::text AS raw_value
                FROM olh_telemetry_latest
                WHERE device_id = :deviceId
                ORDER BY metric
                """)
                .param("deviceId", deviceId)
                .query(this::mapLatest)
                .list();
    }

    public List<TelemetryPoint> historyByDevice(Long deviceId, String metric, OffsetDateTime start, OffsetDateTime end, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        String metricFilter = metric == null || metric.isBlank() ? "" : " AND metric = :metric\n";
        String startFilter = start == null ? "" : " AND time >= :start\n";
        String endFilter = end == null ? "" : " AND time <= :end\n";
        JdbcClient.StatementSpec spec = jdbc.sql("""
                SELECT time, device_id, product_id, metric, numeric_value, text_value, value_type, quality,
                       raw_value::text AS raw_value
                FROM olh_telemetry_data
                WHERE device_id = :deviceId
                """ + metricFilter + startFilter + endFilter + """
                ORDER BY time DESC
                LIMIT :limit
                """)
                .param("deviceId", deviceId)
                .param("limit", boundedLimit);
        if (metric != null && !metric.isBlank()) {
            spec = spec.param("metric", metric);
        }
        if (start != null) {
            spec = spec.param("start", start);
        }
        if (end != null) {
            spec = spec.param("end", end);
        }
        return spec.query(this::mapPoint).list();
    }

    public long countToday() {
        return jdbc.sql("""
                SELECT COUNT(*)
                FROM olh_telemetry_data
                WHERE time >= date_trunc('day', NOW())
                """)
                .query(Long.class)
                .single();
    }

    public List<TelemetryPoint> recent(int limit) {
        return jdbc.sql("""
                SELECT time, device_id, product_id, metric, numeric_value, text_value, value_type, quality,
                       raw_value::text AS raw_value
                FROM olh_telemetry_data
                ORDER BY time DESC
                LIMIT :limit
                """)
                .param("limit", Math.max(1, Math.min(limit, 50)))
                .query(this::mapPoint)
                .list();
    }

    private TelemetryLatest mapLatest(ResultSet rs, int rowNum) throws SQLException {
        return new TelemetryLatest(
                rs.getLong("device_id"),
                rs.getString("metric"),
                null,
                null,
                rs.getObject("time", OffsetDateTime.class),
                rs.getObject("numeric_value", Double.class),
                rs.getString("text_value"),
                rs.getString("value_type"),
                rs.getString("quality"),
                rs.getString("raw_value")
        );
    }

    private TelemetryPoint mapPoint(ResultSet rs, int rowNum) throws SQLException {
        return new TelemetryPoint(
                rs.getObject("time", OffsetDateTime.class),
                rs.getLong("device_id"),
                rs.getLong("product_id"),
                rs.getString("metric"),
                rs.getObject("numeric_value", Double.class),
                rs.getString("text_value"),
                rs.getString("value_type"),
                rs.getString("quality"),
                rs.getString("raw_value")
        );
    }
}
