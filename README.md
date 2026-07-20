# Infotainment Log Console

Infotainment Log Console is a native Android operations app for collecting, caching, reviewing, synchronizing, and exporting simulated subsystem logs. It is written in Kotlin with Jetpack Compose and is deliberately scoped as an inspectable mobile engineering project rather than an in-vehicle deployment.

Public repository: <https://github.com/Praharsh-Projects/infotainment-log-console>

## What it demonstrates

- Kotlin Android development with Jetpack Compose and Material 3.
- MVVM state management with `StateFlow`, `ViewModel`, and coroutines.
- Compose Navigation across live logs, saved logs, and API/session configuration.
- Bearer-authenticated REST integration with OkHttp and typed JSON parsing with kotlinx.serialization.
- A bounded 500-entry JSON cache, legacy TSV migration, filtering, and Android share-sheet export.
- Debug and release endpoint configuration through Gradle properties.
- Unit, lint, build, and emulator regression checks in GitHub Actions.
- A debug APK artifact for test distribution after successful CI checks.

## Mobile workflows

### Local collection

Select a brand and start collection. The ViewModel generates bounded mock signal events on a coroutine, persists them outside the UI thread, and restores them on the next launch.

### Authenticated API sync

The Settings screen accepts a bearer session token and keeps it in process memory only. `GET /v1/logs?brand=...` requests use the build-configured API endpoint, add the authorization header, validate response size and fields, parse JSON, merge duplicate IDs, and cache the result. The repository contains the exact [API contract](docs/api/contract.md) and a [sample response](docs/api/sample-log-page.json).

### Saved-log review

Saved logs can be filtered by brand, severity, text, or source. Exports use the same versioned JSON codec as local persistence and are shared through a scoped `FileProvider` URI.

## Architecture

```text
Compose screens and Navigation
            |
      StateFlow ViewModel
            |
       LogRepository
       /           \
JSON file cache   Authenticated REST client
       |           |
legacy migration  validated JSON response
```

See [docs/architecture.md](docs/architecture.md) for responsibilities, state transitions, and failure handling.

## Run locally

Prerequisites:

- Android Studio or Android SDK 35
- JDK 17
- an Android emulator or device with API 26 or later

If the Android SDK is not already configured in the shell:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

Run automated checks and build the debug APK:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Install on a connected emulator or device:

```bash
./gradlew installDebug
```

Override the emulator API endpoint when needed:

```bash
./gradlew -PdebugApiBaseUrl=http://10.0.2.2:8787/ installDebug
```

Local collection and cache/export flows work without a backend. REST sync requires a server that follows the documented contract.

## Verification scope

- 16 JVM unit tests cover filtering, typed JSON parsing, schema rejection, legacy migration, bounded caching, export, authenticated HTTP requests, response failures, brand isolation, token validation, and merge behavior.
- 1 Compose instrumentation test checks navigation between Live, Settings, and Saved screens on an Android emulator.
- Android Lint and `assembleDebug` run locally and in CI.
- CI uploads test reports and the debug APK only after the quality job succeeds.

## Environment and release boundaries

Debug defaults to `http://10.0.2.2:8787/` for an emulator-hosted API. Release uses a non-routable placeholder unless `releaseApiBaseUrl` is supplied. Signing keys, production credentials, Play Console access, and store submission are not stored or claimed. See [docs/release/release-pipeline.md](docs/release/release-pipeline.md).

## Project layout

```text
app/src/main/java/.../
  data/       JSON cache, REST client, and repository
  domain/     log models, filters, and signal generator
  ui/         ViewModel, Compose screens, navigation, and theme
app/src/test/ JVM regression tests
app/src/androidTest/ Compose emulator smoke test
.github/workflows/ Android quality and test-distribution pipeline
docs/         architecture, API contract, and release boundaries
```

## Limitations

- Signals are simulated and do not come from vehicle hardware or a CAN bus.
- The documented REST API is an integration contract, not a production service bundled in this repository.
- The session token is runtime-only; a production app would integrate an identity SDK and encrypted credential storage.
- The CI artifact is a debug APK for testing, not a signed Play Store release.
- Store publication and physical-device coverage remain external release activities.
