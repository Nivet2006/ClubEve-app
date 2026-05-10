# ClubEve CC

An Android app for PR (Public Relations) officers to manage event attendance at college events — scan QR codes, check in students manually, and track attendance in real time.

---

## What It Does

PR officers use ClubEve CC to check in registered students at events. The app works both online and offline, so check-ins are never lost even without a network connection.

---

## Features

### Event Management
- See all events assigned to you with registration and attendance counts
- Live countdown timer on upcoming events
- "Last synced" timestamp so you always know how fresh the data is

### QR Code Scanner
- Point the camera at a student's QR code — check-in happens instantly, no tap required
- Flash overlay shows the student's name, USN, and check-in status after each scan
- Haptic and audio feedback so you don't need to look at the screen
- Session counter tracks total check-ins for the current session

### Manual Check-In
- Look up any student by their USN when QR scanning isn't possible
- Confirmation card shows student details before writing the check-in

### Attendee List
- View all registrants for an event with check-in status and timestamps
- Filter by All / Present / Absent
- Search by name or USN
- Live updates via Supabase Realtime + auto-refresh every 10 seconds

### Offline Support
- Full offline check-in — records are saved locally when there's no internet
- Pending check-ins sync automatically to the server when connectivity is restored
- Offline badge on the home screen shows how many check-ins are waiting to sync
- Manual sync trigger with a result summary (synced / failed / conflicts)

### Secure Login
- Role-based access — only PR officers can log in
- Optional biometric (fingerprint/face) unlock
- Credentials stored with AES-256 encryption

### Notifications
- Get notified when you're assigned to a new event, even when the app is closed
- Real-time assignment alerts + background polling every 15 minutes as a fallback

### In-App Updates
- Automatically checks for a newer version on every launch
- Shows a download prompt if an update is available
- Manual version check button on the login screen

### Light & Dark Mode
- Full light and dark theme support
- Smooth circular wipe animation when switching themes

---

## How It Works (Brief)

The app talks to a Supabase backend (PostgreSQL + Auth). Events, registrations, and student profiles are cached locally in a Room database so the app stays functional without internet. Background sync is handled by WorkManager.

---

## Requirements

- Android 8.0 or newer (API 26+)
- Camera permission for QR scanning
- Internet connection for initial login and sync (offline check-in works without it)
