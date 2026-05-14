# ClubEve App [![Build & Release](https://github.com/Nivet2006/ClubEve-app/actions/workflows/release.yml/badge.svg)](https://github.com/Nivet2006/ClubEve-app/actions/workflows/release.yml)

An Android app for managing event attendance at college events. PR officers scan student QR codes or look up students manually to mark attendance. Students can view their registered events, QR codes, and attendance history. Club Coordinators get a dedicated dashboard with per-event reporting.

---

## What It Does

Three roles, one app:

- **PR officers** — check in registered students at events via QR scan or manual USN lookup. Works fully offline; check-ins sync to the server automatically when connectivity is restored.
- **Students** — view their registered events, check-in status, QR codes, and full attendance history.
- **Club Coordinators (CC)** — view a dashboard of their club's events and drill into per-event attendance reports. *(Routes defined; screens in progress.)*

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
- Role-based access — accepted roles: `pr`, `student`, `cc`, `teacher`, `hod`, `manager`, `admin`; any other role is rejected at login
- Role routing: `pr` → PR officer home; `student` → student home; `cc` → CC dashboard; `teacher` / `hod` / `manager` / `admin` → Faculty dashboard. Routing applies both at login and when the app is relaunched with an existing session.
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

### Faculty / Staff Flow

- **`FacultyDashboard`** (`faculty_dashboard`) — entry point for `teacher`, `hod`, `admin`, and `manager` roles. Fully implemented:
  - Top bar with "FACULTY REVIEW" title, full name + role subtitle, refresh button, and logout
  - Time-of-day greeting ("Good Morning / Afternoon / Evening") with the user's first name and department
  - **Pending Actions** section — events awaiting the logged-in user's approval action, with an amber left-border accent and count badge:
    - `teacher` → events with status `pending_teacher` filtered to the user's department
    - `hod` → events with status `pending_hod` filtered to the user's department
    - `admin` / `manager` → all events with status `pending_teacher` or `pending_hod` across all departments
  - **Verified & Live** section — events the user has already acted on (`pending_hod` or `approved`), with a green left-border accent; filtered by department for teacher/HOD roles, all departments for admin/manager
  - Each event card shows title, date/time, targeted department, and a color-coded status pill (PUBLISHED / HOD PENDING / PENDING)
  - Search bar (toggle) to filter events by title across both the Pending Actions and Verified & Live sections simultaneously; the section count badge and empty-state messages update to reflect the active search query
  - Pull-to-refresh, logout confirmation dialog, and glassmorphism easter egg (6-tap title trigger)
  - Logout support via `logout(onDone)`
- **`FacultyEventDetail`** (`faculty_event_detail/{eventId}`) — per-event detail screen for faculty/staff. Route defined; screen not yet built.

---

### Club Coordinator (CC) Flow

- **`CcDashboard`** (`cc_dashboard`) — entry point for CC users. Fully implemented:
  - Top bar with "MY PIPELINE" title, coordinator name subtitle, search toggle, refresh, and logout
  - Pipeline stats grid: DRAFTS / IN REVIEW / APPROVED / REJECTED counts with color-coded icons
  - Scrollable event list with pull-to-refresh; each row shows event title, date/time, and a color-coded status pill (Draft / In Review / Approved / Rejected)
  - Search bar (toggle) to filter events by title or club name
  - Logout confirmation dialog
- **`CcEventDetail`** (`cc_event_detail/{eventId}`) — per-event detail screen. Fully implemented:
  - **Approval pipeline stepper** — 5-step visual stepper (Draft → PR Review → Teacher → HOD → Approved); rejected events show a distinct error banner instead
  - **Rejection remarks** — when an event is rejected, structured revision remarks (field + reason) are displayed in a card
  - **Event info** — title, club, date/time, location, registration deadline, description, plus a stats row showing registered count, capacity, and feedback question count
  - **Feedback toggle** — for approved events, a switch to open/close student feedback collection; optimistic update with revert on failure
  - **Submit for review** — for draft and rejected events, a button to advance the status to `pending_teacher`; rejected events show "RESUBMIT FOR REVIEW"
- **`CcReport`** (`cc_report/{eventId}`) — activity report editor; screen not yet built
- **`CcFeedbackEditor`** (`cc_feedback_editor/{eventId}`) — feedback question editor for a specific event. Fully implemented:
  - Top bar with "FEEDBACK QUESTIONS" title, event name subtitle, and a **SAVE** button (enabled only when there are unsaved changes)
  - Add, edit, remove, and **drag-to-reorder** questions via the `sh.calvin.reorderable` library; haptic feedback on drag start and reorder
  - Seven question types: Short Text, Long Text, Rating (1–5), Multiple Choice, Checkboxes, Yes/No, Dropdown
  - Options field (comma-separated) shown only for choice-based types (Multiple Choice, Checkboxes, Dropdown)
  - Required toggle per question
  - Validation on save: minimum 3 questions required; all questions must have a non-blank label
  - Empty state and drag-hint banner when more than one question exists
- **`CcLiveView`** (`cc_live_view/{eventId}`) — live attendance view for a specific event. Fully implemented:
  - Top bar with "LIVE VIEW" title, event name subtitle, and a pulsing green "LIVE" indicator dot
  - Summary bar showing four real-time counts: TOTAL / REGISTERED / SCANNED / PRESENT
  - Scrollable attendee list sorted by status (PRESENT → SCANNED → REGISTERED), then alphabetically by name
  - Each row shows an avatar initial, full name, USN, and a color-coded status badge
  - Three statuses: **PRESENT** (checked in + feedback submitted, green), **SCANNED** (checked in, no feedback yet, amber), **REGISTERED** (not yet scanned, neutral)
  - Live updates via Supabase Realtime — registration changes trigger a full list refresh; new feedback submissions flip the individual attendee's status in-place without a full reload

**Approval pipeline statuses** (`ApprovalStatus`): `draft` → `pending_pr` → `pending_teacher` → `pending_hod` → `approved` / `rejected`. `PENDING_STATUSES` groups all three review stages for aggregate counts.

---

### Splash Screen

- Animated full-screen splash plays once on every app launch (~4.15 s total)
- Timeline: text fades in on a white background (700 ms) → holds on white (800 ms) → background and text color cross-fade to dark (1200 ms) → holds at dark (600 ms) → background fades out while text shrinks from 28 sp → 22 sp and letter spacing narrows from 6 sp → 2 sp (600 ms, morphing into the login title) → text fades out (250 ms)
- In glassmorphism mode the dark target is the deep purple-blue radial gradient used throughout the app; in normal mode it fades to solid black
- Implemented as a stateless `SplashScreen` composable in `ui/components/SplashScreen.kt`; accepts a single `onDone` callback invoked when the animation completes
- The navigation host (and therefore the login/biometric flow) is not rendered until the splash finishes — prevents the auth UI from flashing beneath the splash overlay

---

### Theme

- Full light and dark mode with a smooth circular wipe animation (bottom-right FAB)
- **Theme persistence** — dark mode and glassmorphism on/off state are saved to DataStore (`theme_prefs`) and restored automatically on next launch via `ThemePrefsStore`
- **Glassmorphism mode** — deep purple-blue translucent palette; toggled by tapping the screen title 6 times within 3 seconds (easter egg, available in the PR officer flow via "MY EVENTS", the student flow via "MY EVENTS", and the CC Dashboard via "MY PIPELINE"). The accent color is dynamic — `GlassState.glassAccentColor` drives the entire glass color scheme at runtime, so changing the accent recomposes the theme instantly without a restart.
- **Glass accent color picker** — in glassmorphism mode, the bottom-right FAB switches from the dark/light toggle to a palette icon. Tapping it opens a `GlassColorPickerDialog` with a full HSV color wheel (hue ring, saturation/value square, brightness slider, live preview swatch), all styled to match the glass palette. The chosen color is persisted via `GlassColorStore` and restored on next launch.
- The bottom-right theme FAB and the bottom-left attendance FAB (student flow) are both visible in all modes — normal, dark, and glassmorphism. In glass mode the attendance FAB uses `GlassSurface` background, `GlassBorderColor` border, and the current glass accent color for its icon tint.


---

## Requirements

- Android 8.0 or newer (API 26+)
- Camera permission for QR scanning
- Internet for initial login and sync (offline check-in works without it)

