# Project Structure

## Module Layout
Single-module Android app. All source lives under `app/src/main/java/com/clubeve/cc/`.

```
app/src/main/java/com/clubeve/cc/
├── auth/                        # Auth helpers (not a screen)
│   ├── BiometricHelper.kt       # Biometric prompt wrapper
│   └── CredentialStore.kt       # AES-256 encrypted USN/password storage
├── data/
│   ├── local/                   # Room database
│   │   ├── AppDatabase.kt
│   │   ├── dao/                 # DAOs — return Flow for reactive queries
│   │   └── entity/              # Room @Entity classes
│   ├── remote/
│   │   ├── SupabaseClientProvider.kt  # Singleton Supabase client
│   │   └── dto/                 # @Serializable DTOs for Supabase responses
│   └── repository/              # Business logic; owns sync between remote & local
├── models/                      # Shared domain models (used across UI + repo layers)
├── sync/
│   └── AttendanceSyncWorker.kt  # WorkManager worker for background sync
├── ui/
│   ├── attendance/              # Attendee list screen + ViewModel
│   ├── components/              # Reusable Composables (Button, Card, StatusPill, etc.)
│   ├── events/                  # Home screen, Event detail screen + ViewModels
│   ├── login/                   # Login screen + ViewModel
│   ├── navigation/              # NavGraph + Screen sealed class
│   ├── scanner/                 # QR scanner screen + ViewModel
│   └── theme/                   # Color.kt, Type.kt, Theme.kt
├── utils/
│   └── NetworkMonitor.kt        # Connectivity check
├── ClubEveApplication.kt        # Application class (WorkManager init)
├── MainActivity.kt              # Single activity; hosts NavHost
└── SessionManager.kt            # In-memory session singleton (userId, etc.)
```

## Architecture: MVVM + Repository

- **UI layer**: Composable screens observe `StateFlow` from ViewModels. No business logic in Composables.
- **ViewModel**: Holds `UiState` data class via `MutableStateFlow`. Uses `viewModelScope` for coroutines.
- **Repository**: Mediates between Supabase (remote) and Room (local). Handles offline-first sync logic.
- **Data sources**: `SupabaseClientProvider` (singleton) for remote; Room DAOs for local.

## Naming Conventions

| Artifact | Convention | Example |
|---|---|---|
| ViewModel | `<Feature>ViewModel` | `ScannerViewModel` |
| UI state | `<Feature>UiState` | `ScannerUiState` |
| Screen composable | `<Feature>Screen` | `ScannerScreen` |
| Room entity | `<Name>Entity` | `RegistrationEntity` |
| Remote DTO | `<Name>Dto` | `RegistrationDto` |
| Nav route | `Screen.<Name>` sealed object | `Screen.Scanner` |
| Result types | Sealed class | `ScanResult.AlreadyPresent` |

## Key Patterns

- **State**: Each ViewModel exposes a single `UiState` data class via `StateFlow`, updated with `.update { }`.
- **Navigation**: Type-safe routes via `Screen` sealed class; `AppNavGraph` is the single NavHost.
- **Sync**: Repositories call `syncFromRemote()` on demand; `AttendanceSyncWorker` handles background periodic sync.
- **Singletons**: `SupabaseClientProvider`, `SessionManager` are `object` singletons — avoid adding more unless necessary.
- **Serialization**: DTOs use `@Serializable` (kotlinx.serialization). Entities use Room annotations. Keep them separate — use `.toEntity()` / `.toDto()` extension functions to convert.
- **Secrets**: Always access Supabase credentials via `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`.

## Adding New Features

1. Create a new package under `ui/<feature>/` with `<Feature>Screen.kt` and `<Feature>ViewModel.kt`
2. Add a route to `Screen.kt` and wire it in `AppNavGraph.kt`
3. If the feature needs data, add DTOs in `data/remote/dto/`, entities in `data/local/entity/`, a DAO in `data/local/dao/`, and a repository in `data/repository/`
4. Register new Room entities in `AppDatabase.kt` and bump the schema version
5. Add any new dependencies to `gradle/libs.versions.toml` first, then reference via `libs.*` in `build.gradle.kts`
