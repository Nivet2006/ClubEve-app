# Tech Stack

## Language & Platform
- **Kotlin** (100%, no Java) targeting Android
- **Min SDK**: 26 (Android 8.0) | **Target/Compile SDK**: 35
- **Java 17** compilation target

## Build System
- **Gradle** with Kotlin DSL (`build.gradle.kts`)
- **Version catalog**: `gradle/libs.versions.toml` — all dependency versions are defined here; always reference via `libs.*` aliases
- **KSP** (Kotlin Symbol Processing) for annotation processing (Room)
- Secrets (`SUPABASE_URL`, `SUPABASE_ANON_KEY`) are read from `local.properties` and injected as `BuildConfig` fields — never hardcode them

## Key Libraries & Versions

| Category | Library | Version |
|---|---|---|
| UI | Jetpack Compose BOM | 2024.11.00 |
| UI | Material 3 | (via BOM) |
| Navigation | Navigation Compose | 2.8.4 |
| Async | Kotlin Coroutines | 1.9.0 |
| Database | Room | 2.6.1 |
| Background | WorkManager | 2.9.1 |
| Backend | Supabase (postgrest, auth, realtime) | 3.0.3 |
| Networking | Ktor Client (OkHttp) | 3.0.1 |
| Serialization | Kotlinx Serialization JSON | 1.7.3 |
| Camera | CameraX | 1.4.1 |
| Barcode | ML Kit Barcode Scanning | 17.3.0 |
| Images | Coil Compose | 2.7.0 |
| Permissions | Accompanist Permissions | 0.36.0 |
| Security | androidx.security:security-crypto | 1.1.0-alpha06 |
| Security | androidx.biometric | 1.1.0 |
| Storage | DataStore Preferences | 1.1.1 |

## Common Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Check for dependency updates / lint
./gradlew lint
```

## Configuration
- `local.properties` (git-ignored) must contain:
  ```
  SUPABASE_URL=https://your-project.supabase.co
  SUPABASE_ANON_KEY=your-anon-key
  ```
