#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:18080/api/ingest/http/demo-device-001/telemetry" \
  -H "Content-Type: application/json" \
  -H "X-Device-Secret: demo-secret" \
  -d '{
    "values": {
      "temperature": 36.8,
      "humidity": 62,
      "voltage": 220.1
    }
  }'

echo
