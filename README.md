# ClubEve App

An Android app for PR (Public Relations) officers at a college/university to manage event attendance via QR code scanning and manual check-in.

## Overview

PR officers use ClubEve App to check in registered students at events. The app supports both online and offline operation — check-ins made without a network connection are queued locally and synced to Supabase in the background.

## Features

- **Event list** — Shows events assigned to the logged-in PR officer, with registration and attendance counts
- **QR scanner** — Continuous camera-based QR scanning with auto-confirm: check-ins are written immediately on scan with no confirmation tap required. A brief flash overlay shows the result after each scan, and a session batch counter tracks total check-ins
- **Manual check-in** — Look up a student by USN when QR scanning isn't possible
- **Attendee list** — View all registrants for an event with check-in status and timestamps
- **Offline support** — Full offline check-in via local Room cache; events, registrations, and student profiles are cached locally after each successful home screen load so the app remains functional without a network connection. Pending check-ins are synced to Supabase in the background via WorkManager when connectivity is restored
- **Secure auth** — Role-based login (PR officers only), optional biometric unlock, AES-256 encrypted credential storage

## Tech Stack

| Category | Library / Tool |
|---|---|
| Language | Kotlin (100%) |
| Platform | Android (min SDK 26, target SDK 35) |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.8.4 |
| Async | Kotlin Coroutines 1.9.0 |
| Local DB | Room 2.6.1 |
| Background sync | WorkManager 2.9.1 |
| Backend | Supabase (postgrest, auth, realtime) 3.0.3 |
| Networking | Ktor Client (OkHttp) 3.0.1 |
| Serialization | Kotlinx Serialization JSON 1.7.3 |
| Camera | CameraX 1.4.1 |
| Barcode | ML Kit Barcode Scanning 17.3.0 |
| Images | Coil Compose 2.7.0 |
| Security | androidx.security-crypto + androidx.biometric |

## Architecture

MVVM + Repository pattern.

- **UI layer** — Composable screens observe `StateFlow` from ViewModels. No business logic in Composables.

### ScannerViewModel

`ScannerViewModel` handles both QR and manual USN check-in flows. The two tabs behave differently:

**QR tab — continuous / auto-confirm mode**
`onQrScanned` is called by the camera analyzer on every detected frame. It auto-confirms the check-in immediately without requiring a user tap. A 5-second per-token cooldown (`recentTokens` map) prevents the same QR code from being processed twice while the camera is still pointing at it. After each scan the ViewModel emits a transient `ScanFlash` overlay (student name, USN, and status) that the UI displays briefly before clearing. A running `batchCount` in `ScannerUiState` tracks the total check-ins for the current session. The camera stays active between scans — it is never paused in this mode.

**Manual USN tab — lookup + explicit confirm**
`findByManualUsn` looks up the student and populates `scanResult` (a `ScanResult`). The UI then shows a `ConfirmCard` and the officer taps "Check In" to call `confirmCheckIn`, which writes the record. This tab still uses the confirm-before-write pattern.

Both operations accept a `Context` parameter passed to `NetworkMonitor` at call time to decide whether to route through the online (Supabase) or offline (Room cache) path. This keeps the ViewModel free of Android context as a field while still supporting the offline-first strategy.

**`ScanFlash`** is a lightweight data class emitted on the `scanFlash` field of `ScannerUiState` after each QR scan. It carries `studentName`, `studentUsn`, `isAlreadyCheckedIn`, `isOffline`, `isError`, and an optional `errorMessage`. The UI auto-dismisses the flash after 2 seconds via a `LaunchedEffect`-scoped `delay`, then calls `clearFlash()`.

**`ScanFlashCard`** is the private composable that renders the flash overlay. It slides up from the bottom of the camera view and displays one of three visual states — success (green), already-checked-in (amber), or error (red) — using colour-coded dot indicators and monospaced labels. For offline check-ins it shows an amber `CloudOff` banner identical in style to the one on `ConfirmCard`. The card is non-blocking: it never pauses the camera or requires a user tap.

**`ScanFeedback`** (`utils/ScanFeedback.kt`) is called inside the same `LaunchedEffect` on every new flash. `ScanFeedback.success(context)` fires for a clean check-in; `ScanFeedback.warning(context)` fires for errors and already-checked-in results. This gives the officer immediate haptic and audio confirmation without looking at the screen.

The camera preview is now **unconditionally active** in the QR tab — the previous `state.cameraActive` guard has been removed. The hint text "Point at student's QR code" is shown only when no flash is visible (`state.scanFlash == null`), replacing the old `state.scanResult == null && !state.isProcessing` condition.

### ConfirmCard component

`ConfirmCard` is the bottom-sheet-style card shown after a manual USN lookup. It handles three states driven by `ScanResult`: a new check-in confirmation (with Cancel / Check In buttons), an already-checked-in notice, and an error message.

It accepts an optional `isOffline: Boolean` parameter (default `false`) that callers set when the device has no network connectivity. When `true`, the new check-in confirmation view displays an amber inline banner — a `CloudOff` icon followed by "Saved offline — will sync automatically when online" — so the officer knows the check-in is queued locally and will be pushed to Supabase once connectivity is restored. The banner is not shown for already-checked-in or error states.
- **ViewModel** — Holds a single `UiState` data class via `MutableStateFlow`. Uses `viewModelScope` for coroutines.
- **Repository** — Mediates between Supabase (remote) and Room (local). Handles offline-first sync logic.
- **Data sources** — `SupabaseClientProvider` singleton for remote; Room DAOs for local.

### HomeViewModel

`HomeViewModel` extends `AndroidViewModel` so it can access the Room database and `NetworkMonitor` directly. After a successful remote load it:

1. Upserts fetched events into the local `events` table.
2. Fetches all registrations for each event and upserts them into `registrations`, preserving the `pendingSync` flag on any rows that have an unsynced local check-in.
3. Fetches the corresponding student profiles and upserts them into `profiles`.

It also exposes `pendingSyncCount: StateFlow<Int>` (backed by `SyncManager.observePendingCount`) so the home screen can display a live badge of offline check-ins waiting to be pushed.

### Global offline banner

`AppNavGraph` observes `NetworkMonitor.isOnlineFlow` and renders an amber banner at the top of every screen (except login) whenever the device has no connectivity. The banner shows a `WifiOff` icon and the message "Offline — check-ins will sync when online". Because it lives in the nav graph rather than in any individual screen, it appears consistently regardless of which screen the officer is currently on.

### HomeScreen offline UX

The home screen surfaces offline state in two additional ways beyond the global banner:

- **Sync badge** — When `pendingSyncCount > 0`, a red badge on the cloud-upload icon in the top bar shows the number of pending check-ins.
- **Sync dialog** — Tapping the badge opens a confirmation dialog. Confirming triggers `SyncManager.syncPendingCheckIns` immediately and shows a snackbar with the result (`✓ Synced N check-in(s)` or `⚠ N failed — try again`).

## Local Database Schema

The Room database (`clubeve_cc.db`, schema version 2) contains the following tables:

### `events`
Cached event data for events assigned to the logged-in PR officer.

| Column | Type | Notes |
|---|---|---|
| `id` | String (PK) | Supabase event ID |
| `title` | String | |
| `description` | String | |
| `date` | Long | Unix timestamp (ms) |
| `venue` | String | |
| `status` | String | e.g. `upcoming`, `ongoing`, `completed` |
| `maxCapacity` | Int | |
| `registeredCount` | Int | Default 0 |

### `registrations`
Student registrations for events. Foreign key on `eventId` → `events.id` (CASCADE delete).

| Column | Type | Notes |
|---|---|---|
| `id` | String (PK) | Supabase registration ID |
| `eventId` | String (FK) | References `events.id` |
| `studentId` | String | Supabase user ID |
| `studentName` | String | |
| `usn` | String | University Serial Number |
| `email` | String | |
| `qrToken` | String? | Token encoded in the student's QR code |
| `isPresent` | Boolean | Whether the student has been checked in |
| `markedAt` | Long? | Unix timestamp of check-in |
| `checkedInAt` | String? | ISO timestamp from remote |
| `registeredAt` | String? | ISO timestamp from remote |
| `isSynced` | Boolean | Whether the record matches the remote state |
| `pendingSync` | Boolean | `true` = offline check-in waiting to be pushed |

### `profiles`
Cached student profile data.

| Column | Type | Notes |
|---|---|---|
| `id` | String (PK) | Supabase user ID |
| `fullName` | String | |
| `usn` | String | University Serial Number |
| `department` | String | |
| `semester` | Int | |
| `year` | Int | |
| `role` | String | e.g. `student`, `pr_officer` |

## Project Structure

```
app/src/main/java/com/clubeve/cc/
├── auth/                   # BiometricHelper, CredentialStore (AES-256)
├── data/
│   ├── local/              # Room database, DAOs, entities
│   ├── remote/             # SupabaseClientProvider, DTOs
│   └── repository/         # EventRepository, AttendanceRepository
├── models/                 # Shared domain models
├── sync/                   # AttendanceSyncWorker (WorkManager)
├── ui/
│   ├── attendance/         # Attendee list screen + ViewModel
│   ├── components/         # Reusable Composables
│   ├── events/             # Home screen, Event detail + ViewModels
│   ├── login/              # Login screen + ViewModel
│   ├── navigation/         # AppNavGraph, Screen sealed class
│   ├── scanner/            # QR scanner screen + ViewModel
│   └── theme/              # Color, Type, Theme
├── utils/                  # NetworkMonitor
├── ClubEveApplication.kt
├── MainActivity.kt
└── SessionManager.kt
```

## Setup

1. Clone the repository.
2. Create `local.properties` in the project root (git-ignored) and add your Supabase credentials:
   ```
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   ```
3. Open in Android Studio and sync Gradle.
4. Run on a device or emulator with API 26+.

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

# Lint
./gradlew lint

# Clean
./gradlew clean
```

## Backend

Supabase (PostgreSQL + Auth). Key tables: `pr_event_assignments`, `registrations`, `events`, `profiles`.

Credentials are injected at build time via `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY` — never hardcoded.
