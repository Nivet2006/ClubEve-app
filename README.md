# ClubEve App [![Build & Release](https://github.com/Nivet2006/ClubEve-app/actions/workflows/release.yml/badge.svg)](https://github.com/Nivet2006/ClubEve-app/actions/workflows/release.yml)

An Android app for managing event attendance at college events. PR officers scan student QR codes or look up students manually to mark attendance. Students can view their registered events, QR codes, and attendance history.

---

## What It Does

Two roles, one app:

- **PR officers** — check in registered students at events via QR scan or manual USN lookup. Works fully offline; check-ins sync to the server automatically when connectivity is restored.
- **Students** — view their registered events, check-in status, QR codes, and full attendance history.

---

## Features

### PR Officer Flow

**Event Management**
- See all events assigned to you with registration and attendance counts
- Live countdown timer on each upcoming event card
- "Last synced" timestamp in the top bar, refreshed every 30 seconds

**QR Code Scanner**
- Point the camera at a student's QR code — check-in is written instantly, no tap required
- 5-second per-token cooldown prevents duplicate scans
- Flash overlay shows student name, USN, and status after each scan (auto-dismisses after 2s)
- Haptic and audio feedback: double-beep for success, buzz for errors
- Session counter tracks total check-ins for the current session
- **Live PR presence** — when multiple PR officers are scanning the same event simultaneously, each officer can see the scan counts of other connected PRs in real time via Supabase Realtime Presence. Presence is best-effort; the scanner works normally if the channel is unavailable

**Manual Check-In**
- Look up any student by USN when QR scanning isn't possible
- Confirmation card shows student details before writing the check-in
- Offline banner on the card when the device has no connectivity

**Attendee List**
- View all registrants for an event with check-in status and timestamps
- Filter by All / Present / Absent / Registered
- Search by name or USN
- Live updates via Supabase Realtime + silent auto-refresh every 10 seconds
- Sync status badge: Live / Syncing… / Offline — cached data

**Offline Support**
- Full offline check-in — records saved locally when there's no internet
- Pending check-ins sync automatically when connectivity is restored
- Sync badge on the home screen shows how many check-ins are waiting
- Manual sync dialog with result summary (synced / failed / conflicts)
- Conflict resolution dialog when a remote timestamp differs from the local one

**Secure Login**
- Role-based access — PR officers and students only; any other role is rejected
- Optional biometric (fingerprint/face/PIN) unlock
- Credentials stored with AES-256 GCM encryption
- Screenshots and screen recording blocked on every screen (`FLAG_SECURE`)

**In-App Updates**
- Silently checks GitHub Releases on every launch
- Shows a download prompt if a newer APK is available
- Manual version check button on the login screen

---

### Student Flow

**My Events**
- See all events you're registered for with check-in status (Registered / ✓ Checked In)
- Event details: title, club name, date/time, location
- Tap any card to view your QR code for that event

**Attendance History**
- Floating button opens a bottom sheet with your full attendance history
- Each record shows event title, club, date, check-in time, and status:
  - **ATTENDED** — you were scanned in but haven't submitted feedback yet
  - **PRESENT** — you were scanned in and have submitted feedback

---

### Theme

- Full light and dark mode with a smooth circular wipe animation (bottom-right FAB)
- **Theme persistence** — dark mode and glassmorphism on/off state are saved to DataStore (`theme_prefs`) and restored automatically on next launch via `ThemePrefsStore`
- **Glassmorphism mode** — deep purple-blue translucent palette; toggled by tapping the "MY EVENTS" title 6 times within 3 seconds (easter egg, available in both PR and student flows). The accent color is dynamic — `GlassState.glassAccentColor` drives the entire glass color scheme at runtime, so changing the accent recomposes the theme instantly without a restart.
- **Glass accent color picker** — in glassmorphism mode, the bottom-right FAB switches from the dark/light toggle to a palette icon. Tapping it opens a `GlassColorPickerDialog` with a full HSV color wheel (hue ring, saturation/value square, brightness slider, live preview swatch), all styled to match the glass palette. The chosen color is persisted via `GlassColorStore` and restored on next launch.
- The bottom-right theme FAB and the bottom-left attendance FAB (student flow) are both visible in all modes — normal, dark, and glassmorphism. In glass mode the attendance FAB uses `GlassSurface` background, `GlassBorderColor` border, and the current glass accent color for its icon tint.


---

## Requirements

- Android 8.0 or newer (API 26+)
- Camera permission for QR scanning
- Internet for initial login and sync (offline check-in works without it)

---

## Tech Stack

| Category | Library |
|---|---|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.8.4 |
| Local DB | Room 2.6.1 |
| Background sync | WorkManager 2.9.1 |
| Backend | Supabase 3.0.3 (Auth + Postgrest + Realtime) |
| Camera | CameraX 1.4.1 + ML Kit Barcode 17.3.0 |
| Security | security-crypto 1.1.0-alpha06 + biometric 1.1.0 |
| Storage | DataStore Preferences 1.1.1 |
