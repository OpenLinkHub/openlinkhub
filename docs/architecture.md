# Architecture

The first OpenLinkHub MVP is a modular monolith plus a web console.

```text
Vue Console
    |
Spring Boot Core API
    |
TimescaleDB
```

The backend keeps clear module boundaries:

```text
product      product and thing model metadata
device       device registry, secrets, and bound sensors
telemetry    HTTP ingest, latest data, history data
rule         threshold rules
alarm        alarm lifecycle
dashboard    summary and recent activity
```

`gateway` is intentionally not a separate service yet. HTTP ingest lives in the core API during MVP. A dedicated gateway can be introduced when MQTT, TCP, Modbus, OPC UA, or protocol plugins become part of the runtime.

## Data Flow

```text
Device HTTP POST
    -> device lookup and optional secret validation
    -> write olh_telemetry_data hypertable, one row per sensor metric
    -> upsert olh_telemetry_latest
    -> evaluate enabled threshold rules
    -> create olh_alarm when matched
```
