package com.openlinkhub.iot.device;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class SensorRepository {

    private final JdbcClient jdbc;

    public SensorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Sensor> findByProductId(Long productId) {
        return jdbc.sql(baseSelect() + " WHERE product_id = :productId ORDER BY id")
                .param("productId", productId)
                .query(this::mapSensor)
                .list();
    }

    public Optional<Sensor> findById(Long id) {
        return jdbc.sql(baseSelect() + " WHERE id = :id")
                .param("id", id)
                .query(this::mapSensor)
                .optional();
    }

    public Sensor create(Long productId, SensorRequest request) {
        Long id = jdbc.sql("""
                INSERT INTO olh_sensor_definition (product_id, name, sensor_key, sensor_type, unit, description)
                VALUES (:productId, :name, :sensorKey, COALESCE(:sensorType, 'number'), :unit, :description)
                RETURNING id
                """)
                .param("productId", productId)
                .param("name", request.name())
                .param("sensorKey", request.sensorKey())
                .param("sensorType", request.sensorType())
                .param("unit", request.unit())
                .param("description", request.description())
                .query(Long.class)
                .single();
        return findById(id).orElseThrow();
    }

    public Sensor update(Long id, SensorRequest request) {
        jdbc.sql("""
                UPDATE olh_sensor_definition
                SET product_id = COALESCE(:productId, product_id),
                    name = :name,
                    sensor_key = :sensorKey,
                    sensor_type = COALESCE(:sensorType, 'number'),
                    unit = :unit,
                    description = :description,
                    updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("productId", request.productId())
                .param("name", request.name())
                .param("sensorKey", request.sensorKey())
                .param("sensorType", request.sensorType())
                .param("unit", request.unit())
                .param("description", request.description())
                .update();
        return findById(id).orElseThrow();
    }

    public void delete(Long id) {
        jdbc.sql("DELETE FROM olh_sensor_definition WHERE id = :id")
                .param("id", id)
                .update();
    }

    private String baseSelect() {
        return """
                SELECT id, product_id, name, sensor_key, sensor_type, unit, description, created_at, updated_at
                FROM olh_sensor_definition
                """;
    }

    private Sensor mapSensor(ResultSet rs, int rowNum) throws SQLException {
        return new Sensor(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getString("name"),
                rs.getString("sensor_key"),
                rs.getString("sensor_type"),
                rs.getString("unit"),
                rs.getString("description"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
