# API

Base URL:

```text
http://localhost:18080
```

Health:

```text
GET /api/system/health
```

Products:

```text
GET  /api/products?keyword=Sensor&category=sensor&page=1&size=10
POST /api/products
GET  /api/products/{id}
PUT  /api/products/{id}
```

Devices:

```text
GET  /api/devices?keyword=demo&productId=1&status=online&page=1&size=10
POST /api/devices
GET  /api/devices/{id}
PUT  /api/devices/{id}
GET  /api/devices/{id}/latest
GET  /api/devices/{id}/telemetry?metric=temperature&start=...&end=...&limit=500
```

Sensor definitions:

```text
GET    /api/products/{productId}/sensors
POST   /api/products/{productId}/sensors
PUT    /api/sensor-definitions/{id}
DELETE /api/sensor-definitions/{id}
```

Sensors are defined on products. Devices inherit the sensor model from their product, and `sensorKey` maps to the telemetry metric name, such as `temperature`, `humidity`, `battery`, or `gps`.

Ingest:

```text
POST /api/ingest/http/{deviceKey}/telemetry
Header: X-Device-Secret: demo-secret
```

Payload:

```json
{
  "timestamp": "2026-06-17T10:30:00+08:00",
  "values": {
    "temperature": 36.8,
    "humidity": 62,
    "voltage": 220.1,
    "battery": 87,
    "gps": "31.2304,121.4737"
  }
}
```

Rules:

```text
GET  /api/rules?keyword=temp&deviceKey=demo-device-001&metric=temperature&enabled=true&page=1&size=10
POST /api/rules
PUT  /api/rules/{id}
POST /api/rules/{id}/enable
POST /api/rules/{id}/disable
```

Alarms:

```text
GET  /api/alarms?keyword=demo&status=open&severity=critical&page=1&size=10
POST /api/alarms/{id}/ack
```

Paged list response:

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```
