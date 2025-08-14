# Project Structure Overview

This document provides a detailed overview of the Phone2PC Cursor Control project structure and architecture.

## Directory Structure

```
phone2PC/
├── README.md                    # Main project documentation
├── PROJECT_STRUCTURE.md         # This file - detailed project overview
├── setup.py                     # Python package setup script
├── quick_start.py               # Quick start helper script
│
├── android/                     # Android application
│   ├── build.gradle            # Root-level Gradle configuration
│   ├── settings.gradle         # Gradle project settings
│   ├── gradle.properties       # Gradle properties
│   │
│   ├── app/                    # Main app module
│   │   ├── build.gradle        # App-level Gradle configuration
│   │   ├── proguard-rules.pro  # ProGuard rules
│   │   │
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml    # App manifest
│   │   │   ├── java/com/phone2pc/cursorcontrol/
│   │   │   │   ├── MainActivity.java      # Main activity
│   │   │   │   ├── MotionTracker.java     # OpenCV motion detection
│   │   │   │   └── UDPSender.java        # UDP communication
│   │   │   │
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       │   └── activity_main.xml  # Main UI layout
│   │   │       └── values/
│   │   │           └── strings.xml        # String resources
│   │   │
│   │   └── proguard-rules.pro
│   │
│   └── opencv/                 # OpenCV Android SDK module
│       └── build.gradle        # OpenCV module configuration
│
├── pc_receiver/                 # PC-side Python receiver
│   ├── cursor_controller.py    # Main cursor control script
│   └── requirements.txt        # Python dependencies
│
└── docs/                       # Documentation
    └── INSTALLATION.md         # Detailed installation guide
```

## Architecture Overview

### System Components

```
┌─────────────────┐    UDP     ┌─────────────────┐
│   Android App   │ ────────→  │   PC Receiver  │
│                 │            │                 │
│ • Camera Input  │            │ • UDP Server    │
│ • Motion Detect │            │ • Cursor Ctrl   │
│ • UDP Client    │            │ • Performance   │
└─────────────────┘            └─────────────────┘
```

### Data Flow

1. **Camera Capture**: Android app captures front camera feed
2. **Motion Detection**: OpenCV processes frames using optical flow
3. **Data Transmission**: Motion data sent via UDP to PC
4. **Cursor Control**: PC receives data and moves mouse cursor
5. **Feedback Loop**: Continuous real-time control

## Key Components

### Android App (`android/app/`)

#### MainActivity.java
- **Purpose**: Main application entry point and UI controller
- **Responsibilities**:
  - Camera setup and permissions
  - UI state management
  - Connection handling
  - Lifecycle management

#### MotionTracker.java
- **Purpose**: Core motion detection engine
- **Key Features**:
  - OpenCV optical flow implementation
  - Lucas-Kanade motion tracking
  - Motion smoothing and filtering
  - Performance optimization
- **Algorithm**: Uses grid-based tracking points for robust motion detection

#### UDPSender.java
- **Purpose**: Network communication layer
- **Features**:
  - Low-latency UDP transmission
  - Connection management
  - Heartbeat mechanism
  - Error handling

### PC Receiver (`pc_receiver/`)

#### cursor_controller.py
- **Purpose**: Main server and cursor control
- **Features**:
  - UDP server implementation
  - Real-time cursor movement
  - Performance monitoring
  - Configurable sensitivity and smoothing
- **Dependencies**: pyautogui, socket, threading

## Technical Specifications

### Performance Targets
- **Latency**: ≤50ms cursor response time
- **Frame Rate**: 20 FPS motion tracking
- **Network**: UDP for low-latency communication
- **Accuracy**: Sub-pixel motion detection

### Motion Detection Algorithm
- **Method**: Lucas-Kanade optical flow
- **Tracking Points**: 20x20 grid (400 points)
- **Smoothing**: Exponential moving average
- **Threshold**: Configurable motion sensitivity

### Network Protocol
- **Transport**: UDP (User Datagram Protocol)
- **Port**: 5000 (configurable)
- **Message Format**: `TYPE:data`
- **Message Types**:
  - `MOTION:dx,dy` - Motion data
  - `CONNECT:client_id` - Connection message
  - `DISCONNECT:client_id` - Disconnection message
  - `HEARTBEAT:timestamp` - Keep-alive message

## Configuration Options

### Android Side
```java
// MotionTracker.java
private static final int TRACKING_INTERVAL_MS = 50;        // 20 FPS
private static final double MOTION_THRESHOLD = 2.0;        // Motion sensitivity
private static final double SENSITIVITY_MULTIPLIER = 2.0;  // Cursor sensitivity
private static final double SMOOTHING_FACTOR = 0.7;        // Motion smoothing

// UDPSender.java
private static final int HEARTBEAT_INTERVAL_MS = 1000;     // Heartbeat frequency
```

### PC Side
```bash
# Command line options
--sensitivity 1.5    # Cursor sensitivity multiplier
--smoothing 0.8      # Motion smoothing factor (0.0-1.0)
--port 5000          # UDP port to listen on
--host 0.0.0.0       # Host to bind to
```

## Development Workflow

### 1. Setup Development Environment
```bash
# Clone repository
git clone <repository-url>
cd phone2PC

# Install Python dependencies
pip install -r pc_receiver/requirements.txt

# Open Android project in Android Studio
# android/ directory
```

### 2. Android Development
- Use Android Studio for development
- Ensure OpenCV SDK is properly integrated
- Test on physical device (emulator may not support camera)
- Monitor logcat for debugging

### 3. PC Receiver Development
- Python 3.7+ required
- Use virtual environment for dependency management
- Test UDP connectivity
- Monitor performance metrics

### 4. Testing
- Test motion detection with various surfaces
- Verify network connectivity
- Measure latency and accuracy
- Test with different Android devices

## Build and Deployment

### Android App
```bash
# Build APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### PC Receiver
```bash
# Install as package
pip install -e .

# Run directly
python pc_receiver/cursor_controller.py

# Run as command
phone2pc-cursor
```

## Troubleshooting Guide

### Common Issues
1. **Camera not working**: Check permissions and device compatibility
2. **Network connection failed**: Verify IP address and firewall settings
3. **High latency**: Check network congestion and device performance
4. **Motion tracking inaccurate**: Adjust sensitivity and smoothing parameters

### Debug Tools
- Android: Logcat, Android Studio debugger
- PC: Python logging, network monitoring tools
- Network: Wireshark, netstat

## Performance Optimization

### Android Optimization
- Efficient OpenCV operations
- Background thread management
- Battery optimization
- Memory management

### PC Optimization
- High-priority process execution
- Efficient cursor movement
- Network buffer optimization
- System resource management

## Security Considerations

- **Network Security**: UDP is unencrypted, use on trusted networks
- **Device Permissions**: Camera and network access required
- **Data Privacy**: Motion data transmitted in real-time
- **Access Control**: No authentication implemented

## Future Enhancements

- **Encryption**: Add TLS/DTLS for secure communication
- **Authentication**: Implement device pairing and verification
- **Multi-device**: Support multiple phones controlling one PC
- **Gesture Recognition**: Advanced motion patterns and gestures
- **Cross-platform**: iOS app development
- **Web Interface**: Browser-based control interface 