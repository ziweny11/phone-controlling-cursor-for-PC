# Android-PC Cursor Control

A real-time cursor control system that uses your smartphone's front camera to control your PC's mouse cursor through motion tracking.

## Features

- **Real-time motion tracking** using OpenCV optical flow
- **Low-latency communication** via UDP protocol (≤50ms response time)
- **Battery-optimized** Android app with efficient sensor management
- **Cross-platform** - Android app + Python PC receiver

## How It Works

1. **Motion Detection**: Android app captures front camera feed and uses OpenCV's optical flow to detect 2D movement relative to the environment
2. **Data Transmission**: Movement data is sent via UDP to the PC
3. **Cursor Control**: Python script receives data and moves the PC mouse cursor accordingly

## Project Structure

```
phone2PC/
├── android/                 # Android application
│   ├── app/                # Main app module
│   ├── opencv/             # OpenCV Android SDK
│   └── build.gradle        # Build configuration
├── pc_receiver/            # PC-side Python receiver
│   ├── cursor_controller.py # Main cursor control script
│   └── requirements.txt    # Python dependencies
├── docs/                   # Documentation
└── README.md              # This file
```

## Setup Instructions

### Android App Setup

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and install on your Android device
4. Grant camera permissions

### PC Receiver Setup

1. Install Python 3.7+
2. Install dependencies: `pip install -r pc_receiver/requirements.txt`
3. Run: `python pc_receiver/cursor_controller.py`

## Usage

1. Start the PC receiver script
2. Launch the Android app
3. Point your phone's front camera at a stable surface
4. Move your phone to control the PC cursor

## Technical Details

- **Motion Algorithm**: Lucas-Kanade optical flow
- **Communication**: UDP socket programming
- **Performance**: Optimized for low latency and battery efficiency
- **Platforms**: Android 6.0+ (API 23+), Windows/macOS/Linux

## Requirements

- Android device with front camera
- PC with Python 3.7+
- Both devices on same network
- OpenCV 4.x 