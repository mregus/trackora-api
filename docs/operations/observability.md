## Observability

Prometheus metrics include:

- Telematics processing
- Dashboard refresh
- Driver safety calculations
- AI request latency
- Tool execution latency
- Tool failures
- WebSocket activity

Grafana dashboards:

- Fleet Overview
- Telematics Pipeline
- Driver Safety
- Fleet Copilot

## Health Check

- `GET /actuator/health`

Expected:

```json
{
    "status": "UP"
}
```

## Grafana

- Runs on port `3001`