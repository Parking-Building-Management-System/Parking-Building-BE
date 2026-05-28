# System Admin System Health API Spec

## Scope

These APIs provide platform health, service dependency status, request traffic, latency, and error visibility for `SYSTEM_ADMIN`.

## Endpoints

### GET /admin/system-health/summary

Purpose: return the current high-level platform health summary.

Response:

```json
{
  "status": "OPERATIONAL",
  "uptimeSeconds": 86400,
  "totalRequests": 124000,
  "errorRate": 0.012,
  "avgLatencyMs": 94,
  "activeTenants": 18,
  "activeSessions": 342
}
```

### GET /admin/system-health/services

Purpose: return health checks for backend services and dependencies.

Service item:

```json
{
  "name": "Database",
  "status": "OPERATIONAL",
  "latencyMs": 18,
  "lastCheckedAt": "2026-05-28T03:30:00Z",
  "message": "Connection pool is healthy"
}
```

Recommended statuses:

- `OPERATIONAL`
- `DEGRADED`
- `DOWN`
- `UNKNOWN`

### GET /admin/system-health/traffic?from=&to=&granularity=

Purpose: return request volume, error count, and latency by time bucket.

Query:

- `from`: ISO timestamp
- `to`: ISO timestamp
- `granularity`: `MINUTE`, `HOUR`, or `DAY`

Traffic point:

```json
{
  "timestamp": "2026-05-28T03:00:00Z",
  "requestCount": 560,
  "errorCount": 5,
  "avgLatencyMs": 88
}
```

### GET /admin/system-health/top-endpoints?from=&to=&limit=

Purpose: return the highest-traffic endpoints in the selected time window.

Top endpoint item:

```json
{
  "method": "POST",
  "path": "/staff/parking-sessions/check-in",
  "requestCount": 618,
  "errorCount": 11,
  "avgLatencyMs": 126
}
```

### GET /admin/system-health/errors?from=&to=

Purpose: return recent platform errors and grouped error counts.

Error item:

```json
{
  "timestamp": "2026-05-28T03:12:00Z",
  "method": "POST",
  "path": "/staff/parking-sessions/check-in",
  "status": 400,
  "errorCode": "NO_AVAILABLE_SLOT",
  "message": "No available slot",
  "count": 8
}
```

## Field Reference

Summary:

- `status`
- `uptimeSeconds`
- `totalRequests`
- `errorRate`
- `avgLatencyMs`
- `activeTenants`
- `activeSessions`

Service item:

- `name`
- `status`
- `latencyMs`
- `lastCheckedAt`
- `message`

Traffic point:

- `timestamp`
- `requestCount`
- `errorCount`
- `avgLatencyMs`

Top endpoint:

- `method`
- `path`
- `requestCount`
- `errorCount`
- `avgLatencyMs`

## Security

- `SYSTEM_ADMIN` only.
- Responses must not include secrets, access tokens, refresh tokens, database credentials, object storage keys, or raw request bodies.
- Aggregate tenant-level metrics are allowed; tenant-sensitive payloads are not.

