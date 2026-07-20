# Log sync API contract

## Request

```http
GET /v1/logs?brand=NordicDrive
Accept: application/json
Authorization: Bearer <session-token>
```

The API base URL is supplied by `debugApiBaseUrl` or `releaseApiBaseUrl` in Gradle. The client requires a non-empty token, never persists it, and does not retry authentication failures.

## Successful response

```json
{
  "entries": [
    {
      "id": 42001,
      "timestamp": "2026-07-21 09:15:00",
      "brand": "NordicDrive",
      "subsystem": "Connectivity",
      "signal": "lteSignal",
      "value": "-83dBm",
      "level": "INFO",
      "source": "REMOTE"
    }
  ]
}
```

Unknown JSON fields are ignored for forward compatibility. Required log fields must be non-empty, IDs must be positive, and `level`/`source` must match the documented enum values. The client accepts no more than 500 entries or 1,000,000 response characters.

## Failure behavior

- Non-2xx: `Log sync failed with HTTP <status>.`
- I/O failure: configured API could not be reached.
- malformed or incompatible JSON: response is rejected.
- oversized payload or entry list: response is rejected before caching.

Response bodies and bearer tokens are never exposed in UI error messages.
