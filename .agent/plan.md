# Project Plan

Club-Eve Attendance Management App. A dark-first, glassmorphic app for Club Coordinators to scan student QR codes and mark attendance. Features offline-first sync with Room, Supabase integration, and Material 3 design.

## Project Brief

# ClubEve CC Project Brief

ClubEve CC is a dedicated attendance management application designed for Club Coordinators (CC) within the ClubEve event management ecosystem. The app focuses on high-efficiency QR code scanning and reliable data synchronization, even in environments with limited connectivity.

## Features
- **Secure Coordinator Authentication:** Integration with Supabase Auth to ensure only authorized Club Coordinators can manage attendance.
- **Offline-First Event Sync:** Capability to download student registration lists for specific events, allowing coordinators to work without an active internet connection.
- **High-Speed QR Attendance Scanner:** Integrated QR code scanning to instantly validate and mark student attendance using their University Serial Number (USN).
- **Background Data Synchronization:** Automated syncing of attendance records back to the central server using WorkManager once connectivity is restored.

## High-Level Technical Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3 (implementing a dark-first, glassmorphic aesthetic).
- **Navigation:** Jetpack Navigation 3 (State-driven architecture).
- **Adaptive Layout:** Compose Material Adaptive library for seamless transitions across different screen sizes.
- **Concurrency:** Kotlin Coroutines & Flow.
- **Local Persistence:** Room Database for offline storage of registration lists and scanned attendance.
- **Background Processing:** WorkManager for reliable data synchronization.
- **Networking & Auth:** Supabase (Auth and Database) and CameraX for QR scanning.

## Implementation Steps

### Task_1_Foundation_Auth: Configure Supabase integration, Room database schema, and implement the Authentication flow with a dark-first Login screen.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Supabase API keys integrated
  - Room DB initialized for registrations and attendance
  - Login with Supabase Auth functional
  - Project builds successfully

### Task_2_Event_Scanner: Implement event registration fetching, local storage in Room, and the CameraX-based QR scanner to mark student attendance.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Event registrations stored offline in Room
  - QR scanner identifies USN correctly from QR codes
  - Attendance records created locally

### Task_3_Sync_Glassmorphism: Develop WorkManager tasks for background data synchronization and apply the glassmorphic Material 3 theme across the app.
- **Status:** PENDING
- **Acceptance Criteria:**
  - WorkManager syncs attendance to Supabase when online
  - Glassmorphic UI components (blur/transparency) implemented
  - Edge-to-Edge display functional

### Task_4_Finalize_Verify: Configure the adaptive app icon, refine Material 3 color schemes, and perform final verification of the app's stability and requirements.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Adaptive icon matching app function created
  - Vibrant M3 color scheme applied for light/dark modes
  - App verified by critic_agent for stability and UI alignment
  - All tests pass and no crashes reported

