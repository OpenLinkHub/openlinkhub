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
olh_sensor              Device to sensor bindings
olh_telemetry_data      TimescaleDB hypertable
olh_telemetry_latest
olh_rule
olh_alarm
```

`olh_telemetry_data` stores one row per metric per timestamp. This keeps time-range queries and metric filtering simple, while still allowing flexible JSON raw values.

`olh_sensor` models the one-to-many relationship between a physical device and its sensors or observable metrics. A temperature and humidity monitoring device can bind sensors such as:

```text
gps
temperature
humidity
battery
voltage
```

Each sensor's `sensor_key` should match the telemetry metric key used during ingest.
