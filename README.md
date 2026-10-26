# Android FPS Meter

Android FPS Meter overlays true real-time frame rates (FPS), frame times (ms), and display refresh rate (Hz) over any Android application or game (including BGMI, PUBG Mobile, Call of Duty, Genshin Impact, Minecraft, etc.).

It is open source, **100% functional on non-rooted devices**, and fully compatible with **Android 5.0 up to Android 16+**.

---

## 🚀 Key Features

- **True In-Game Real-Time FPS (Non-Root)**: Captures hardware/compositor-level frame presentation timestamps via Android's `MediaProjection` VirtualDisplay engine. Measures actual in-game rendering performance, not just display refresh rate!
- **Works on Non-Rooted Devices**: Zero root, zero PC/ADB setup required. Fully supported on Android 5.0 through Android 16+.
- **Dual Engine Architecture**:
  - **Screen Frame Engine (Non-Root)**: Uses an ultra-lightweight 64x64 RGBA buffer (~16KB per frame) recycled immediately with zero game stutter or battery drain.
  - **SurfaceFlinger IPC Engine (Root)**: Leverages direct SurfaceFlinger IPC transaction counters and dumpsys when root access is present.
- **Rich Performance Metrics HUD**:
  - **Real-Time FPS**: Large color-coded frame rate indicator.
  - **Frame Time (ms)**: Instantaneous frame delivery duration for detecting micro-stutters.
  - **Display Hz Indicator**: Shows active screen refresh rate alongside in-game FPS (e.g. `16.6 ms • 120Hz display`).
- **Color-Coded Feedback**:
  - 🟢 **Green**: Smooth performance (≥55 FPS)
  - 🟡 **Yellow**: Medium performance (30–54 FPS)
  - 🔴 **Red**: Choppy / stuttering (<30 FPS)
  - ⚪ **Gray**: Screen idle / paused (0 FPS)
- **Signed Release APK**: Pre-configured with automated APK signing (v1 and v2 schemes) for direct installation.
- **Android 14, 15 & 16 Ready**: Strictly complies with foreground service types (`mediaProjection` + `specialUse`), notification permissions, and modern activity result contracts.

---

## 📱 Quick Links

- [Installation Guide](INSTALLATION.md)
- [Developer Guide](DEVELOPMENT.md)
- [Signing Setup Guide](SIGNING_SETUP.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Troubleshooting](TROUBLESHOOTING.md)

---

## 🛠️ How It Works

### Non-Root Real-Time Engine (Android 5.0 – 16+)
Standard `Choreographer` callbacks only report the overlay's display VSYNC refresh rate (e.g. constant 60Hz or 120Hz), ignoring whether the foreground 3D game is lagging or rendering at 25 FPS.

To solve this without root, **Android FPS Meter** creates a lightweight 64x64 `VirtualDisplay` backed by an `ImageReader` mirroring the screen compositor:
1. Whenever the active game renders and presents a new frame to SurfaceFlinger, an image available event is received.
2. The buffer is immediately recycled (`image.close()`), consuming negligible memory and CPU.
3. Timestamps are measured over a 1.0-second sliding window via `FPSCalculator`, providing exact real-time in-game FPS and frame times.

---

## 📦 How to Build

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 35/36)

### Build Commands
```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build signed release APK
./gradlew assembleRelease
```

The signed release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

[MIT](LICENSE)
