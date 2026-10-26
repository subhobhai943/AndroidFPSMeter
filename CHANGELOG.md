# Changelog

## v1.0.1 (2026-10-26)
- **Foreground Service**: Fixed Android 14/15 FGS compliance by migrating from `shortService` (which caused hard 3-minute timeouts) to `specialUse`.
- **UI State Synchronization**: Fixed state desynchronization bug in `MainActivity` where `isServiceRunning` remained false and button click listeners were lost after granting permissions.
- **Notification Controls**: Added direct "Stop" action button and tap-to-open PendingIntent to the foreground notification.
- **Android Compatibility**: Fixed `startForegroundService` crash on Android 5.0–7.1 (API 21–25) by using `ContextCompat.startForegroundService`.
- **Notifications on Android 13+**: Added runtime permission handling for `POST_NOTIFICATIONS`.
- **Launcher Icons**: Replaced corrupted base64 launcher icon with valid launcher icons across mdpi, hdpi, xhdpi, xxhdpi, and xxxhdpi densities.
- **Build System**: Generated and committed Gradle wrapper 8.7, executable `gradlew`, and `.gitignore`.
- **Unit Testing**: Added `FPSCalculator` unit tests covering 30/60/120 FPS calculations, sliding window, and bounds.
- **CI/CD**: Added automated GitHub Actions workflows for continuous integration (testing, linting, debug build) and GitHub releases (signed release APK, SHA-256 checksums, and release notes).

## v1.0.0 (2025-10-29)
- Initial public release
- Real-time FPS overlay using Choreographer
- Overlay permission flow and notification channel
- Basic UI with start/stop controls
- Docs: Installation, API, Troubleshooting, Development
