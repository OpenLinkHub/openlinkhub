package com.openlinkhub.iot.device;

import com.openlinkhub.iot.common.PageResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceRepository {

    private final JdbcClient jdbc;

    public DeviceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Device> findAll() {
        return jdbc.sql(baseSelect() + " ORDER BY d.id DESC")
                .query(this::mapDevice)
                .list();
    }

    public PageResult<Device> findPage(String keyword, Long productId, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        String keywordFilter = keyword == null || keyword.isBlank()
                ? ""
                : " AND (d.name ILIKE :keyword OR d.device_key ILIKE :keyword OR d.location ILIKE :keyword)\n";
        String productFilter = productId == null ? "" : " AND d.product_id = :productId\n";
        String statusFilter = status == null || status.isBlank() ? "" : " AND d.status = :status\n";

        JdbcClient.StatementSpec countSpec = jdbc.sql("""
                SELECT COUNT(*)
                FROM olh_device d
                JOIN olh_product p ON p.id = d.product_id
                WHERE TRUE
                """ + keywordFilter + productFilter + statusFilter);
        JdbcClient.StatementSpec dataSpec = jdbc.sql(baseSelect() + """
                WHERE TRUE
                """ + keywordFilter + productFilter + statusFilter + """
                ORDER BY d.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .param("limit", safeSize)
                .param("offset", offset);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            countSpec = countSpec.param("keyword", like);
            dataSpec = dataSpec.param("keyword", like);
        }
        if (productId != null) {
            countSpec = countSpec.param("productId", productId);
            dataSpec = dataSpec.param("productId", productId);
        }
        if (status != null && !status.isBlank()) {
            countSpec = countSpec.param("status", status);
            dataSpec = dataSpec.param("status", status);
        }

        return new PageResult<>(
                dataSpec.query(this::mapDevice).list(),
                countSpec.query(Long.class).single(),
                safePage,
                safeSize
        );
    }

    public Optional<Device> findById(Long id) {
        return jdbc.sql(baseSelect() + " WHERE d.id = :id")
                .param("id", id)
                .query(this::mapDevice)
                .optional();
    }

    public Optional<Device> findByDeviceKey(String deviceKey) {
        return jdbc.sql(baseSelect() + " WHERE d.device_key = :deviceKey")
                .param("deviceKey", deviceKey)
                .query(this::mapDevice)
                .optional();
    }

    public Device create(DeviceRequest request) {
        Long id = jdbc.sql("""
                INSERT INTO olh_device (product_id, name, device_key, secret, connection_config, location)
                VALUES (:productId, :name, :deviceKey, :secret, CAST(:connectionConfig AS jsonb), :location)
                RETURNING id
                """)
                .param("productId", request.productId())
                .param("name", request.name())
                .param("deviceKey", request.deviceKey())
                .param("secret", request.secret() == null || request.secret().isBlank() ? UUID.randomUUID().toString() : request.secret())
                .param("connectionConfig", normalizeJson(request.connectionConfig()))
                .param("location", request.location())
                .query(Long.class)
                .single();
        return findByIdOrThrow(id);
    }

    public Device update(Long id, DeviceRequest request) {
        jdbc.sql("""
                UPDATE olh_device
                SET product_id = :productId,
                    name = :name,
                    device_key = :deviceKey,
                    secret = COALESCE(NULLIF(:secret, ''), secret),
                    connection_config = CAST(:connectionConfig AS jsonb),
                    location = :location,
                    updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("productId", request.productId())
                .param("name", request.name())
                .param("deviceKey", request.deviceKey())
                .param("secret", request.secret())
                .param("connectionConfig", normalizeJson(request.connectionConfig()))
                .param("location", request.location())
                .update();
        return findByIdOrThrow(id);
    }

    public void markSeen(Long deviceId) {
        jdbc.sql("""
                UPDATE olh_device
                SET status = 'online',
                    last_seen_at = NOW(),
                    updated_at = NOW()
                WHERE id = :deviceId
                """)
                .param("deviceId", deviceId)
                .update();
    }

    private Device findByIdOrThrow(Long id) {
        return findById(id).orElseThrow();
    }

    private String baseSelect() {
        return """
                SELECT d.id, d.product_id, p.name AS product_name, p.protocol_type AS product_protocol_type,
                       d.name, d.device_key, d.secret, d.connection_config::text AS connection_config,
                       d.location, d.status, d.last_seen_at, d.created_at, d.updated_at
                FROM olh_device d
                JOIN olh_product p ON p.id = d.product_id
                """;
    }

    private Device mapDevice(ResultSet rs, int rowNum) throws SQLException {
        return new Device(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("product_protocol_type"),
                rs.getString("name"),
                rs.getString("device_key"),
                rs.getString("secret"),
                rs.getString("connection_config"),
                rs.getString("location"),
                rs.getString("status"),
                rs.getObject("last_seen_at", java.time.OffsetDateTime.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private String normalizeJson(String value) {
        return value == null || value.isBlank() ? "{}" : value;
    }
}
