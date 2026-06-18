# OpenLinkHub IoT Core MVP

OpenLinkHub is an open-source connectivity hub for IoT devices, telemetry data, automation rules, alarms, and external APIs.

This MVP uses a Monorepo layout:

```text
openlinkhub
├── backend   Spring Boot 3 / Java 21 API
├── console   Vue 3 Signal Operations Console
├── deploy    Local database and compose helpers
├── docs      Architecture and API notes
└── examples  Device ingest examples
```

## Local Stack

- Java 21
- Maven 3.9+
- Node.js 20+
- TimescaleDB on `localhost:5432`

Default database settings:

```text
database: openlinkhub_iot
username: iot_user
password: iot_password
```

## Start

Create the database:

```bash
PGPASSWORD=iot_password psql -h localhost -p 5432 -U iot_user -d postgres -f deploy/create-database.sql
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Run the console:

```bash
cd console
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Demo Device

The schema seeds one product, one device, and one threshold rule:

```text
deviceKey: demo-device-001
secret: demo-secret
rule: temperature > 35
```

Send telemetry:

```bash
bash examples/curl/send-demo-telemetry.sh
```
