# Build and release pipeline

## Automated test distribution

Every push to `main` and every pull request runs:

1. JVM unit tests.
2. Android Lint.
3. Debug APK assembly.
4. A Compose navigation regression on an API 34 emulator.
5. Upload of unit/emulator reports and the debug APK as 14-day GitHub Actions artifacts.

The APK is intended for internal testing only. It uses Android's debug signing identity and is not a store artifact.

## Environment configuration

| Variant | Gradle property | Default |
| --- | --- | --- |
| Debug | `debugApiBaseUrl` | `http://10.0.2.2:8787/` |
| Release | `releaseApiBaseUrl` | `https://api.example.invalid/` |

Example release compilation with an explicit endpoint:

```bash
./gradlew -PreleaseApiBaseUrl=https://api.company.example/ assembleRelease
```

The release variant enables R8 minification. The placeholder default deliberately prevents an accidental production connection.

## External store gates

These steps require organization-owned credentials and are intentionally not represented as complete:

- configure a protected upload keystore and Gradle signing config;
- provision Play Console application IDs, tracks, and service-account access;
- replace the placeholder release endpoint with an approved HTTPS service;
- run physical-device, accessibility, battery, network-loss, and upgrade testing;
- review privacy, telemetry, retention, and security requirements;
- create signed Android App Bundles and promote them through internal, closed, and production tracks.

No signing key, production token, or Play credential belongs in this repository.
