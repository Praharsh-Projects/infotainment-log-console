# Architecture

## Intent

The app is structured as a small mobile operations console rather than a consumer UI. The goal is to make log collection, review, and export easy to inspect and easy to maintain.

## Main Parts

- **UI layer**  
  Jetpack Compose screen with brand switching, filter controls, collection toggles, summary cards, and a log list.

- **ViewModel**  
  Holds the current collection state, query, active brand, selected severity, and persisted entries. It also owns the background collection loop.

- **Mock signal generator**  
  Produces deterministic brand-specific signals that resemble infotainment or vehicle subsystem data.

- **File store**  
  Persists entries in a compact local text file and exports the visible subset through a `FileProvider`.

- **Pure logic helpers**  
  Encoding, decoding, and filtering are isolated as plain Kotlin logic so they remain easy to test.

## Data Flow

```text
brand selection / collection toggle
    ->
ViewModel
    ->
mock signal generator
    ->
local file persistence
    ->
filtered Compose log viewer
    ->
export/share action
```

## Why This Structure

- Keeps the collection loop easy to understand.
- Avoids heavy persistence layers for a small logging tool.
- Makes the testable logic independent from the Android framework.
- Leaves room to replace mock data with a real transport layer later.
