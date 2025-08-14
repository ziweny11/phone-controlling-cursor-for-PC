# Installation Guide

This guide will walk you through setting up both the Android app and PC receiver for the Phone2PC Cursor Control system.

## Prerequisites

### Android Development
- Android Studio 4.0 or higher
- Android SDK API 23+ (Android 6.0)
- Android device with front camera
- USB debugging enabled on device

### PC Requirements
- Python 3.7 or higher
- Windows 10/11, macOS 10.14+, or Linux
- Both devices on the same network

## Android App Setup

### 1. OpenCV SDK Integration

The Android app requires OpenCV for motion tracking. You have two options:

#### Option A: Use OpenCV Android SDK (Recommended)
1. Download OpenCV Android SDK from [opencv.org](https://opencv.org/releases/)
2. Extract the SDK to the `android/opencv/` directory
3. Copy the OpenCV native libraries to `android/opencv/src/main/jniLibs/`
4. Copy the OpenCV Java classes to `android/opencv/src/main/java/`

#### Option B: Use Maven Repository
1. Add OpenCV dependency to `android/app/build.gradle`:
```gradle
implementation 'org.opencv:opencv-android:4.8.0'
```

### 2. Build and Install

1. Open the project in Android Studio
2. Sync Gradle files
3. Connect your Android device via USB
4. Enable USB debugging on your device
5. Build and run the app
6. Grant camera permissions when prompted

### 3. Configure Network

1. Ensure your Android device and PC are on the same WiFi network
2. Note your PC's IP address (use `ipconfig` on Windows or `ifconfig` on Linux/macOS)
3. Enter the PC's IP address in the Android app

## PC Receiver Setup

### 1. Install Python Dependencies

```bash
# Navigate to the project directory
cd phone2PC

# Install dependencies
pip install -r pc_receiver/requirements.txt

# Or install using setup.py
pip install -e .
```

### 2. Run the Cursor Controller

```bash
# Basic usage
python pc_receiver/cursor_controller.py

# With custom settings
python pc_receiver/cursor_controller.py --sensitivity 1.5 --smoothing 0.8

# Or use the installed command
phone2pc-cursor --sensitivity 1.5
```

### 3. Command Line Options

- `--host`: Host to bind to (default: 0.0.0.0)
- `--port`: Port to listen on (default: 5000)
- `--sensitivity`: Cursor sensitivity multiplier (default: 1.0)
- `--smoothing`: Motion smoothing factor 0.0-1.0 (default: 0.7)

## Testing the System

### 1. Start PC Receiver
```bash
python pc_receiver/cursor_controller.py
```

### 2. Launch Android App
1. Open the Phone2PC Cursor app
2. Enter your PC's IP address
3. Tap "Connect"
4. Tap "Start Tracking"

### 3. Test Motion Control
1. Point your phone's front camera at a stable surface
2. Move your phone slowly to control the cursor
3. The cursor should follow your phone's motion

## Troubleshooting

### Common Issues

#### Android App Won't Connect
- Check that both devices are on the same network
- Verify the PC IP address is correct
- Ensure firewall allows UDP port 5000
- Check Android app logs for connection errors

#### Motion Tracking Not Working
- Ensure camera permissions are granted
- Check that the front camera is working
- Verify OpenCV is properly integrated
- Check motion sensitivity settings

#### High Latency
- Reduce network congestion
- Lower motion sensitivity
- Increase smoothing factor
- Check for background processes

#### Cursor Movement Too Sensitive
- Lower sensitivity in PC receiver: `--sensitivity 0.5`
- Increase smoothing: `--smoothing 0.9`
- Adjust threshold in MotionTracker.java

### Performance Optimization

#### Android Side
- Reduce tracking interval in MotionTracker.java
- Optimize OpenCV parameters
- Use efficient sensor management
- Minimize background processes

#### PC Side
- Run with higher priority
- Close unnecessary applications
- Use wired network connection
- Optimize Python performance

## Advanced Configuration

### Customizing Motion Detection

Edit `android/app/src/main/java/com/phone2pc/cursorcontrol/MotionTracker.java`:

```java
// Adjust tracking parameters
private static final int TRACKING_INTERVAL_MS = 33; // 30 FPS
private static final double MOTION_THRESHOLD = 1.5; // Lower = more sensitive
private static final double SENSITIVITY_MULTIPLIER = 1.5; // Adjust sensitivity
private static final double SMOOTHING_FACTOR = 0.8; // Higher = smoother
```

### Network Configuration

Edit `android/app/src/main/java/com/phone2pc/cursorcontrol/UDPSender.java`:

```java
// Adjust network parameters
private static final int HEARTBEAT_INTERVAL_MS = 500; // More frequent heartbeats
```

## Security Considerations

- The system uses UDP for low latency
- No encryption is implemented by default
- Only use on trusted networks
- Consider implementing authentication for production use
- Monitor network traffic for unusual activity

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review Android Studio logs
3. Check Python console output
4. Verify network connectivity
5. Test with different devices/networks 