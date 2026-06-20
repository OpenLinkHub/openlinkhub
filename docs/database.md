# Database

The MVP uses TimescaleDB as the main store.

Default connection:

```text
jdbc:postgresql://localhost:5432/openlinkhub_iot
user: iot_user
password: iot_password
```

Core tables:

```text
olh_product
olh_device
olh_sensor_definition   Product sensor definitions
olh_telemetry_data      TimescaleDB hypertable
olh_telemetry_latest
olh_rule
olh_alarm
```

`olh_telemetry_data` stores one row per metric per timestamp. This keeps time-range queries and metric filtering simple, while still allowing flexible JSON raw values.

`olh_sensor_definition` models the one-to-many relationship between a product and its sensor types. Devices only select a product and inherit the same sensor definition set. A temperature and humidity monitoring product can define sensors such as:

```text
gps
temperature
humidity
battery
voltage
```

Each sensor's `sensor_key` should match the telemetry metric key used during ingest. The legacy `olh_sensor` table can remain during migration, but new CRUD operations use `olh_sensor_definition`.
