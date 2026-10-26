# Development Guide

## Setting Up Development Environment

### Prerequisites

1. **Android Studio**: Latest stable version (Flamingo or newer)
2. **Android SDK**: API levels 21-34
3. **Kotlin**: Version 1.9.22 or newer
4. **Git**: For version control

### Project Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/subhobhai943/AndroidFPSMeter.git
   cd AndroidFPSMeter
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to cloned directory

3. **Gradle Sync**
   - Wait for initial Gradle sync to complete
   - Resolve any dependency issues
   - Ensure all SDKs are downloaded

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/fpsmeter/app/
│       │   ├── MainActivity.kt
│       │   ├── FPSMeterService.kt
│       │   ├── FPSOverlayView.kt
│       │   └── PermissionHelper.kt
│       ├── res/
│       │   ├── layout/
│       │   ├── values/
│       │   └── drawable/
│       └── AndroidManifest.xml
└── build.gradle
```

### Code Organization

- **MainActivity**: UI controller and service management
- **FPSMeterService**: Background service handling
- **FPSOverlayView**: Custom view with FPS monitoring logic
- **PermissionHelper**: Permission management utility

## Key Development Concepts

### Android Choreographer API

The core of FPS monitoring uses Choreographer.FrameCallback:

```kotlin
class FPSOverlayView : LinearLayout, Choreographer.FrameCallback {
    private val choreographer = Choreographer.getInstance()

    override fun doFrame(frameTimeNanos: Long) {
        // FPS calculation logic
        choreographer.postFrameCallback(this)
    }
}
```

### System Overlay Implementation

Using WindowManager for overlay display:

```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
    PixelFormat.TRANSLUCENT
)
windowManager.addView(overlayView, params)
```

### Foreground Service Pattern

Service implementation for persistent overlay:

```kotlin
class FPSMeterService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        showOverlay()
        return START_STICKY
    }
}
```

## Building and Testing

### Debug Build

```bash
./gradlew assembleDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

### Running Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Installing on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Customization and Extension

### Adding New Metrics

1. **Extend FPSOverlayView**
   ```kotlin
   class FPSOverlayView : LinearLayout {
       private val cpuTextView: TextView
       private val memoryTextView: TextView

       private fun updateMetrics() {
           // Add CPU and memory monitoring
       }
   }
   ```

2. **Update Layout**
   ```xml
   <LinearLayout>
       <TextView android:id="@+id/fps_text" />
       <TextView android:id="@+id/cpu_text" />
       <TextView android:id="@+id/memory_text" />
   </LinearLayout>
   ```

### Customizing Appearance

1. **Colors and Themes**
   - Modify `res/values/colors.xml`
   - Update color thresholds in `updateFPSDisplay()`

2. **Text and Fonts**
   - Change text size in FPSOverlayView initialization
   - Add custom fonts to `res/fonts/`

3. **Positioning**
   - Modify WindowManager.LayoutParams in `showOverlay()`
   - Add user preferences for position

### Performance Monitoring

Add additional system metrics:

```kotlin
private fun getCPUUsage(): Float {
    // Implement CPU usage monitoring
}

private fun getMemoryUsage(): Long {
    // Implement memory usage monitoring
}

private fun getTemperature(): Float {
    // Implement temperature monitoring
}
```

## Code Quality Guidelines

### Kotlin Style Guide

- Follow [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- Use meaningful variable names
- Add KDoc comments for public methods
- Prefer immutable data structures

### Error Handling

```kotlin
try {
    windowManager.addView(overlayView, params)
    isOverlayVisible = true
} catch (e: SecurityException) {
    Log.e(TAG, "Overlay permission denied", e)
    // Handle permission error
} catch (e: WindowManager.BadTokenException) {
    Log.e(TAG, "Invalid window token", e)
    // Handle window error
}
```

### Memory Management

- Remove Choreographer callbacks in `onDestroy()`
- Clear collections to prevent memory leaks
- Use weak references where appropriate

## Testing Strategy

### Unit Tests

Test FPS calculation logic:

```kotlin
@Test
fun testFPSCalculation() {
    val fps = calculateFPS(frameTimes)
    assertTrue(fps in 0.0..240.0)
}
```

### Integration Tests

Test service and overlay interaction:

```kotlin
@Test
fun testServiceOverlayIntegration() {
    // Start service
    // Verify overlay appears
    // Test FPS updates
}
```

### Manual Testing

1. **Device Testing**
   - Test on various Android versions
   - Test different screen sizes and densities
   - Verify on different manufacturers

2. **Game Testing**
   - Test with popular games (BGMI, PUBG, etc.)
   - Verify FPS accuracy against known benchmarks
   - Test performance impact

## Contributing Guidelines

### Pull Request Process

1. **Fork Repository**
2. **Create Feature Branch**
   ```bash
   git checkout -b feature/new-feature
   ```
3. **Make Changes**
   - Follow coding standards
   - Add tests for new functionality
   - Update documentation

4. **Test Changes**
   - Run all tests
   - Test on real devices
   - Verify no regressions

5. **Submit Pull Request**
   - Clear description of changes
   - Link to related issues
   - Include testing information

### Code Review Checklist

- [ ] Code follows Kotlin style guide
- [ ] No memory leaks introduced
- [ ] Error handling is appropriate
- [ ] Tests cover new functionality
- [ ] Documentation is updated
- [ ] Performance impact is minimal

## Release Process

### Version Management

Update version in `app/build.gradle`:

```gradle
defaultConfig {
    versionCode 2
    versionName "1.1"
}
```

### Creating Release

1. **Update Version Numbers**
2. **Run Full Test Suite**
3. **Build Release APK**
4. **Create GitHub Release**
5. **Update Documentation**

### Release Checklist

- [ ] Version numbers updated
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Release notes prepared
- [ ] APK signed and tested
- [ ] GitHub release created

## Debugging Tips

### Logcat Filtering

```bash
adb logcat -s FPSMeter
adb logcat | grep -i choreographer
```

### Common Debug Scenarios

1. **Overlay Not Showing**
   - Check WindowManager.addView() exceptions
   - Verify permission status
   - Test overlay parameters

2. **FPS Calculation Issues**
   - Log frame timing data
   - Verify Choreographer callbacks
   - Check calculation algorithm

3. **Service Lifecycle Problems**
   - Monitor service start/stop events
   - Check notification creation
   - Verify foreground service behavior

### Performance Profiling

Use Android Studio Profiler to monitor:
- CPU usage during FPS monitoring
- Memory allocation patterns
- Network activity (should be none)

## Future Enhancements

### Planned Features

1. **Customizable Overlay Position**
   - Drag-and-drop positioning
   - Save user preferences
   - Multiple preset positions

2. **Additional Metrics**
   - CPU usage monitoring
   - Memory usage display
   - Device temperature
   - Battery usage

3. **Historical Data**
   - FPS history graphs
   - Performance logging
   - Export data functionality

4. **Game-Specific Profiles**
   - Per-game settings
   - Automatic game detection
   - Optimized display per game

### Architecture Improvements

1. **MVVM Pattern Implementation**
2. **Dependency Injection with Hilt**
3. **Room Database for Settings**
4. **Coroutines for Background Tasks**

This development guide provides the foundation for contributing to and extending the Android FPS Meter project.
