# Changelog

## v1.1.0 (2026-10-26)
- **Real-Time In-Game FPS Engine**: Added hardware-level frame monitoring using SurfaceFlinger IPC and dumpsys gfxinfo to measure actual in-game rendered FPS (rather than display VSync refresh rate).
- **Active Game Detection**: Automatically detects foreground game/application package name and displays it in the overlay.
- **GPU Load Monitoring**: Added real-time GPU load percentage metrics via kernel sysfs.
- **Pre-configured Release Signing**: Included bundled permanent `release.keystore` ensuring every release APK built locally or via GitHub Actions is 100% cryptographically signed with v1, v2, and v3 signatures out of the box.
- **Repository Clean-up**: Renamed repository and all documentation references from `AndroidFPSMeter-v2` to `AndroidFPSMeter`.

## v1.0.1 (2026-10-26)
- **Foreground Service**: Fixed Android 14/15 FGS compliance by migrating from `shortService` to `specialUse`.
- **UI State Synchronization**: Fixed state desynchronization bug in `MainActivity`.
- **Notification Controls**: Added direct "Stop" action button to the notification.
- **Android Compatibility**: Fixed `startForegroundService` crash on Android 5.0–7.1 (API 21–25).
- **Notifications on Android 13+**: Added runtime permission handling for `POST_NOTIFICATIONS`.
- **Launcher Icons**: Replaced corrupted base64 launcher icon with valid PNG icons.
- **Build System**: Generated and committed Gradle wrapper 8.7 and `.gitignore`.
- **CI/CD**: Added automated GitHub Actions workflows for continuous integration and releases.

## v1.0.0 (2025-10-29)
- Initial public release
