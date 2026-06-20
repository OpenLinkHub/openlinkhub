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
product      product metadata, default protocol, and generated thing model view
sensor       product-level sensor definitions
device       device registry, product selection, secrets, and connection config
telemetry    HTTP ingest, latest data, history data by sensor key
rule         threshold rules
alarm        alarm lifecycle
dashboard    summary and recent activity
```

`gateway` is intentionally not a separate service yet. HTTP ingest lives in the core API during MVP. A dedicated gateway can be introduced when MQTT, TCP, Modbus, OPC UA, or protocol plugins become part of the runtime.

## Data Flow

```text
Device HTTP POST
    -> device lookup and optional secret validation
    -> validate the product protocol allows the current ingest path
    -> resolve the device product's sensor definitions for latest status display
    -> write olh_telemetry_data hypertable, one row per sensor metric
    -> upsert olh_telemetry_latest
    -> evaluate enabled threshold rules
    -> create olh_alarm when matched
```
