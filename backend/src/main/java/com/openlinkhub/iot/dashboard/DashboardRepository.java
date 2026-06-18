package com.openlinkhub.iot.dashboard;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final JdbcClient jdbc;

    public DashboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long productCount() {
        return count("SELECT COUNT(*) FROM olh_product");
    }

    public long deviceCount() {
        return count("SELECT COUNT(*) FROM olh_device");
    }

    public long onlineDeviceCount() {
        return count("SELECT COUNT(*) FROM olh_device WHERE status = 'online'");
    }

    public long openAlarmCount() {
        return count("SELECT COUNT(*) FROM olh_alarm WHERE status = 'open'");
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}
