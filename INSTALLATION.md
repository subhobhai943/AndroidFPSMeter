# Installation and Setup Guide

## Prerequisites

- Android device running Android 5.0 (API level 21) or higher
- Android Studio (for building from source)
- USB debugging enabled on your Android device

## Option 1: Install APK (Recommended)

1. **Download the APK** from the releases section of this repository
2. **Enable Unknown Sources** on your Android device:
   - Go to Settings > Security
   - Enable "Unknown sources" or "Install unknown apps"
3. **Install the APK** by tapping on the downloaded file
4. **Launch the app** and follow the permission setup below

## Option 2: Build from Source

### Steps:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/subhobhai943/AndroidFPSMeter.git
   cd AndroidFPSMeter
   ```

2. **Open in Android Studio:**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Build the project:**
   - Wait for Gradle sync to complete
   - Click "Build" > "Build Bundle(s) / APK(s)" > "Build APK(s)"
   - The APK will be generated in `app/build/outputs/apk/debug/`

4. **Install on device:**
   - Connect your Android device via USB
   - Enable USB debugging in Developer Options
   - Click "Run" in Android Studio or install the APK manually

## Permission Setup

### Required Permissions:

1. **System Alert Window (Overlay Permission):**
   - This is the most critical permission for the app to function
   - When you first launch the app, it will request this permission
   - You'll be redirected to system settings to grant overlay permission

2. **Notification Permission (Android 13+):**
   - Required for the foreground service notification
   - The app will request this automatically on Android 13 and above

### Step-by-step Permission Grant:

1. **Launch the FPS Meter app**
2. **Tap "Grant Permission"** when prompted
3. **In the system settings:**
   - Find "FPS Meter" in the app list
   - Toggle "Permit drawing over other apps" to ON
   - Press back to return to the app
4. **The app should now show "Ready to start FPS monitoring"**

## Usage Instructions

### Starting FPS Monitoring:

1. **Open the FPS Meter app**
2. **Tap "Start FPS Meter"**
3. **A notification will appear** indicating the service is running
4. **The FPS overlay will appear** on your screen (usually top-right corner)
5. **Open any game or app** to monitor its FPS

### Understanding the FPS Display:

- **Green numbers**: FPS ≥ 55 (Excellent performance)
- **Yellow numbers**: FPS 30-54 (Good performance)
- **Red numbers**: FPS < 30 (Poor performance)

### Stopping FPS Monitoring:

1. **Return to the FPS Meter app**
2. **Tap "Stop FPS Meter"**
3. **The overlay will disappear** and the service will stop

## Troubleshooting

### App Won't Start:
- Ensure your device runs Android 5.0 or higher
- Check if the APK is corrupted and re-download
- Clear app data and try again

### Permission Issues:
- Go to Settings > Apps > FPS Meter > Permissions
- Ensure "Display over other apps" is enabled
- Some devices may have additional overlay restrictions

### Overlay Not Showing:
- Check if the overlay permission is granted
- Restart the app after granting permissions
- Some devices may have power saving modes that interfere

### Inaccurate FPS Readings:
- The FPS shown is for the system UI, not necessarily the game
- Some games may have their own FPS limiting
- Device performance can affect readings

### Battery Optimization:
- Some devices may kill the background service
- Go to Settings > Battery > Battery Optimization
- Find "FPS Meter" and set it to "Don't optimize"

## Testing with Games

### Popular Games to Test:
- **BGMI (Battlegrounds Mobile India)**
- **PUBG Mobile**
- **Call of Duty Mobile**
- **Genshin Impact**
- **Free Fire**
- **Any graphics-intensive game**

### Expected Results:
- Modern flagship devices: 60-120 FPS in most games
- Mid-range devices: 30-60 FPS depending on game settings
- Older devices: 20-30 FPS in demanding games

## Device Compatibility

### Tested Devices:
- Samsung Galaxy series (Android 8.0+)
- OnePlus devices (Android 8.0+)
- Xiaomi/Redmi devices (MIUI 10+)
- Realme devices (ColorOS 6+)
- Google Pixel devices (Android 8.0+)

### Known Issues:
- Some Chinese ROM variants may have additional overlay restrictions
- Gaming mode on some devices may interfere with overlay display
- Very old devices (Android 5.0-6.0) may have limited functionality

## Support

If you encounter any issues:
1. Check this troubleshooting guide first
2. Create an issue on GitHub with device details and error description
3. Include Android version, device model, and steps to reproduce

## Advanced Usage

### For Developers:
- The app uses Choreographer.FrameCallback for FPS measurement
- Overlay is implemented using WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
- Service runs as a foreground service to prevent killing

### Customization:
- Fork the repository to modify overlay appearance
- Adjust FPS calculation intervals in FPSOverlayView.kt
- Change color thresholds in the updateFPSDisplay() method