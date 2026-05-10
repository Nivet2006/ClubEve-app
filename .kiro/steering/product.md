# Product: ClubEve CC

ClubEve CC is an Android app for PR (Public Relations) officers at a college/university to manage event attendance.

## Core Purpose
PR officers use this app to check in registered students at events by scanning their QR codes or looking them up manually by USN (University Serial Number).

## Key Features
- **Event list**: Shows events assigned to the logged-in PR officer, with registration and attendance counts
- **QR scanner**: Camera-based QR code scanning to mark a student as present
- **Manual check-in**: Look up a student by USN when QR scanning isn't possible
- **Attendee list**: View all registrants for an event with check-in status and timestamps
- **Offline support**: Local Room cache with background sync via WorkManager when network is available
- **Secure auth**: Role-based login (PR officers only), optional biometric unlock, AES-256 encrypted credential storage

## Target Users
PR officers managing physical event check-ins. Not a general-purpose event app — access is restricted to assigned officers.

## Backend
Supabase (PostgreSQL + Auth). Key tables: `pr_event_assignments`, `registrations`, `events`.
