# Architecture

## Design goal

The app separates mobile presentation, state transitions, storage, and network I/O so each responsibility can be tested or replaced without rewriting the rest of the client.

## Layers

### Domain

`SignalLogEntry`, `LogLevel`, `LogSource`, brand definitions, filters, and the simulated signal generator contain no Android UI dependencies. Filters and serialization behavior are covered by JVM tests.

### Data

`DefaultLogRepository` presents one interface to the ViewModel.

- `JsonLogFileStore` persists at most 500 entries, exports versioned JSON, and migrates the original TSV cache on first load.
- `HttpLogRemoteDataSource` sends a bearer-authenticated REST request with OkHttp, rejects invalid endpoints/tokens, bounds payload and entry counts, parses typed JSON, validates required fields, and maps entries to the remote source.
- `mergeLogs` deduplicates by ID, orders entries by timestamp, and enforces the cache bound.

Storage and HTTP calls run on `Dispatchers.IO`.

### Presentation

`LogConsoleViewModel` owns an immutable `LogUiState` exposed as `StateFlow`. It coordinates cache loading, background collection, API sync, filtering, clear/export actions, loading flags, and user-readable failures. Compose screens observe lifecycle-aware state rather than owning business data.

The navigation graph has three routes:

- **Live**: brand selection, simulated collection, API sync, and recent entries.
- **Saved**: query/severity filters, JSON export, and cache clearing.
- **Settings**: the build-configured endpoint and an in-memory bearer token.

## State and data flow

```text
user action
    -> ViewModel intent
    -> repository operation on coroutine
    -> validated domain entries
    -> bounded cache update
    -> StateFlow update
    -> Compose recomposition
```

The background collector is a cancellable `viewModelScope` job. Starting it twice is ignored; pausing or clearing cancels the active job. Remote sync also prevents overlapping requests.

## Error handling

- Empty or oversized tokens are rejected before the request.
- Non-2xx responses produce bounded status messages without echoing response bodies or secrets.
- JSON syntax, schema versions, response size, entry count, and required fields are validated.
- Cache and export errors return to the UI through state rather than crashing the screen.
- The session token is not persisted, logged, or included in exported JSON.

## Trade-offs

The file cache avoids Room schema tooling for a small append/read workload and makes migration/export behavior easy to inspect. A larger product would likely use Room, encrypted token storage, pagination, retry/backoff policy, and dependency injection. Those boundaries are documented instead of implied as completed work.
