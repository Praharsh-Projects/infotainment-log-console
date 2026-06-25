# Infotainment Log Console

A small Android app built with Kotlin and Jetpack Compose to simulate infotainment-style logging and data collection workflows across multiple vehicle brands.

The app is intentionally narrow and portfolio-safe. It focuses on:

- background collection of mock signal events
- local persistence of collected logs
- log filtering and review
- export and share of collected data
- lightweight multi-brand configuration through theme and brand switching

## What It Demonstrates

- Android app structure in Kotlin
- Jetpack Compose UI for operational tooling
- state-driven background collection controls
- maintenance-friendly logging and export flows
- file-based local persistence
- unit-tested parsing and filtering logic

## Core Features

- **Mock signal collection**  
  Generates vehicle or infotainment-style signal entries such as speed, media volume, cabin temperature, and connectivity state.

- **Background toggle**  
  Start or stop collection without losing previously persisted entries.

- **Local persistence**  
  Collected entries are stored in app-local storage and reloaded on the next launch.

- **Log viewer and filters**  
  Filter by severity, active brand, or free-text query.

- **Export and share**  
  Export the current visible log set to a text file and share it through Android's standard share sheet.

- **Brand switch**  
  Toggle between two brand profiles to mimic multi-brand configuration behavior.

## Tech Stack

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Coroutines
- Gradle
- JUnit

## Run Locally

### Prerequisites

- Android SDK installed
- JDK 17
- An emulator or device

### Build

```bash
./gradlew test
./gradlew assembleDebug
```

### Install to a connected device or emulator

```bash
./gradlew installDebug
```

## Project Layout

```text
app/
  src/main/
  src/test/
docs/
```

## Files Worth Reviewing

- [docs/architecture.md](docs/architecture.md)
- [docs/samples/exported-log-sample.txt](docs/samples/exported-log-sample.txt)
- [docs/screenshots/infotainment-log-console-home.png](docs/screenshots/infotainment-log-console-home.png)

## Resume-Safe Positioning

- Built a Kotlin Android app with Jetpack Compose that simulates infotainment data logging, local persistence, log filtering, and export workflows across multiple vehicle brands.
- Implemented background collection controls, maintenance-friendly file storage, and unit-tested parsing and filtering logic for a mobile diagnostic workflow.

## Limitations

- This is a simulated mobile logging tool, not an in-vehicle infotainment deployment.
- Data collection is mocked and deterministic rather than sourced from hardware or a CAN bus.
- Persistence uses a compact app-local file store to keep the implementation easy to inspect.
