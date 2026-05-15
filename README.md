# ClubEve CC

**ClubEve CC** is an Android application for PR (Public Relations) officers at colleges/universities to manage event attendance efficiently through QR code scanning and manual check-ins.

## 🎯 Purpose

ClubEve CC streamlines the event check-in process by allowing PR officers to:
- Scan student QR codes for instant attendance marking
- Manually look up students by USN when QR scanning isn't available
- View real-time attendance statistics
- Work offline with automatic background sync

## ✨ Key Features

### For PR Officers
- **Event Dashboard**: View assigned events with registration and attendance counts
- **QR Scanner**: Camera-based QR code scanning for quick check-ins
- **Manual Check-in**: Search and mark attendance by USN
- **Attendee List**: View all registrants with check-in status and timestamps
- **Offline Support**: Local caching with background sync via WorkManager

### For Students
- **Event Registration**: Browse and register for upcoming events
- **QR Code Display**: Generate personal QR code for event check-in
- **Attendance History**: Track attendance across all registered events

### For Faculty & Coordinators
- **Event Management**: Create and manage events
- **Live Attendance**: Real-time attendance monitoring during events
- **Reports & Analytics**: Comprehensive attendance reports and statistics
- **Feedback Collection**: Gather and analyze event feedback

### Security
- **Role-based Access**: Secure authentication with role-specific features
- **Biometric Login**: Optional fingerprint/face unlock
- **Encrypted Storage**: AES-256 encrypted credential storage
- **Screenshot Protection**: Prevents screenshots and screen recording

## 🛠️ Tech Stack

### Core
- **Language**: Kotlin (100%)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **Build System**: Gradle with Kotlin DSL

### Architecture
- **Pattern**: MVVM + Repository
- **UI**: Jetpack Compose with Material 3
- **Navigation**: Navigation Compose
- **Async**: Kotlin Coroutines + Flow
- **DI**: Manual dependency injection with singletons

### Key Libraries
| Category | Library | Version |
|----------|---------|---------|
| UI | Compose BOM | 2024.11.00 |
| Navigation | Navigation Compose | 2.8.4 |
| Database | Room | 2.6.1 |
| Backend | Supabase | 3.0.3 |
| Networking | Ktor Client | 3.0.1 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Camera | CameraX | 1.4.1 |
| Barcode | ML Kit | 17.3.0 |
| Images | Coil | 2.7.0 |
| Security | Security Crypto | 1.1.0-alpha06 |
| Background | WorkManager | 2.9.1 |

## 📁 Project Structure

```
app/src/main/java/com/clubeve/cc/
├── auth/                    # Authentication helpers
│   ├── BiometricHelper.kt   # Biometric prompt wrapper
│   └── CredentialStore.kt   # Encrypted credential storage
├── data/
│   ├── local/               # Room database
│   │   ├── AppDatabase.kt
│   │   ├── dao/             # Data Access Objects
│   │   └── entity/          # Room entities
│   ├── remote/
│   │   ├── SupabaseClientProvider.kt
│   │   └── dto/             # Data Transfer Objects
│   └── repository/          # Business logic layer
├── models/                  # Domain models
├── ui/
│   ├── attendance/          # Attendee list screen
│   ├── cc/                  # Club Coordinator screens
│   ├── components/          # Reusable composables
│   ├── events/              # Event list & detail screens
│   ├── faculty/             # Faculty screens
│   ├── login/               # Login screen
│   ├── navigation/          # Navigation graph
│   ├── scanner/             # QR scanner screen
│   ├── student/             # Student screens
│   └── theme/               # Theme & styling
├── utils/                   # Utilities (NetworkMonitor, ScanFeedback, ShakeDetector)
├── ClubEveApplication.kt    # Application class
├── MainActivity.kt          # Single activity host
└── SessionManager.kt        # Session management
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35
- Supabase account

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ClubEve-app
   ```

2. **Configure Supabase**
   
   Create `local.properties` in the project root:
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   ```

3. **Sync Gradle**
   ```bash
   ./gradlew build
   ```

4. **Run the app**
   - Connect a device or start an emulator
   - Click Run in Android Studio or:
   ```bash
   ./gradlew installDebug
   ```

## 🔨 Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

## 🎨 Features in Detail

### Offline-First Architecture
- All data is cached locally using Room
- Background sync via WorkManager when network is available
- Seamless offline/online transitions
- Conflict resolution for concurrent updates

### Theme System
- **Light/Dark Mode**: System-aware with manual toggle
- **Glass Mode**: Translucent surfaces with radial gradient background
- **Custom Accents**: Configurable accent colors in glass mode
- **Smooth Transitions**: Animated theme switching

### QR Code System
- **Generation**: Unique QR codes per student registration
- **Scanning**: ML Kit barcode scanning with camera preview
- **Validation**: Server-side validation with duplicate prevention
- **Fallback**: Manual USN lookup when scanning fails

### Security Features
- **Encrypted Storage**: AES-256-GCM for credentials
- **Biometric Auth**: Fingerprint/face unlock support
- **Screenshot Protection**: FLAG_SECURE on all screens
- **Role-based Access**: Server-enforced role permissions

## 📱 User Roles

### PR Officer
- Assigned to specific events
- Can mark attendance via QR or manual lookup
- View attendee lists and statistics

### Student
- Register for events
- Generate QR code for check-in
- View attendance history

### Club Coordinator (CC)
- Create and manage events
- Monitor live attendance
- Generate reports and collect feedback

### Faculty
- View events by department
- Access attendance reports
- Monitor event statistics

## 🔐 Security Notes

- Never commit `local.properties` or keystore files
- Supabase credentials are injected at build time via BuildConfig
- All API calls use Row Level Security (RLS) policies
- Biometric data never leaves the device

## 📄 License

[Add your license here]

## 👥 Contributors

[Add contributors here]

## 📞 Support

For issues or questions, please [open an issue](link-to-issues) or contact [support-email].

---

**Built with ❤️ using Jetpack Compose**
