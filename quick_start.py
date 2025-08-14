#!/usr/bin/env python3
"""
Quick Start Script for Phone2PC Cursor Control
This script helps you get the system running quickly.
"""

import os
import sys
import subprocess
import socket
import platform
import time

def print_banner():
    """Print the application banner"""
    print("=" * 60)
    print("           Phone2PC Cursor Control - Quick Start")
    print("=" * 60)
    print()

def check_python_version():
    """Check if Python version is compatible"""
    if sys.version_info < (3, 7):
        print("Python 3.7 or higher is required!")
        print(f"   Current version: {sys.version}")
        sys.exit(1)
    print(f"Python version: {sys.version.split()[0]}")

def check_dependencies():
    """Check if required Python packages are installed"""
    print("\nChecking Python dependencies...")
    
    required_packages = ['pyautogui', 'opencv-python', 'numpy', 'psutil']
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package.replace('-', '_'))
            print(f"   {package}")
        except ImportError:
            print(f"   {package}")
            missing_packages.append(package)
    
    if missing_packages:
        print(f"\nInstalling missing packages: {', '.join(missing_packages)}")
        try:
            subprocess.check_call([sys.executable, '-m', 'pip', 'install'] + missing_packages)
            print("Dependencies installed successfully!")
        except subprocess.CalledProcessError:
            print("Failed to install dependencies. Please run manually:")
            print(f"   pip install {' '.join(missing_packages)}")
            sys.exit(1)
    else:
        print("All dependencies are installed!")

def get_local_ip():
    """Get the local IP address of this machine"""
    try:
        # Create a socket to get local IP
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            local_ip = s.getsockname()[0]
        return local_ip
    except Exception:
        return "127.0.0.1"

def check_network():
    """Check network connectivity"""
    print("\nChecking network configuration...")
    
    local_ip = get_local_ip()
    print(f"   Local IP: {local_ip}")
    
    # Check if port 5000 is available
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.bind((local_ip, 5000))
            print("   Port 5000 is available")
    except OSError:
        print("   Port 5000 is already in use")
        print("   You may need to stop other applications using this port")
    
    return local_ip

def start_cursor_controller():
    """Start the cursor controller"""
    print("\nStarting Phone2PC Cursor Controller...")
    
    # Check if the cursor controller script exists
    script_path = os.path.join("pc_receiver", "cursor_controller.py")
    if not os.path.exists(script_path):
        print("Cursor controller script not found!")
        print("   Please ensure you're in the correct directory")
        sys.exit(1)
    
    try:
        # Start the cursor controller
        print("   Starting UDP server on port 5000...")
        print("   Press Ctrl+C to stop the server")
        print()
        
        # Run the cursor controller
        subprocess.run([sys.executable, script_path])
        
    except KeyboardInterrupt:
        print("\n\nCursor controller stopped by user")
    except Exception as e:
        print(f"\nError starting cursor controller: {e}")

def show_android_instructions(local_ip):
    """Show instructions for Android app setup"""
    print("\nAndroid App Setup Instructions:")
    print("=" * 50)
    print("1. Install the Android app on your phone")
    print("2. Ensure both devices are on the same WiFi network")
    print(f"3. Open the app and enter this IP address: {local_ip}")
    print("4. Tap 'Connect' to establish connection")
    print("5. Tap 'Start Tracking' to begin motion control")
    print("6. Point your phone's front camera at a stable surface")
    print("7. Move your phone to control the PC cursor")
    print()
    print("Tips:")
    print("   - Keep your phone steady for better tracking")
    print("   - Use slow, deliberate movements")
    print("   - Ensure good lighting conditions")
    print("   - Keep the camera focused on a textured surface")

def main():
    """Main function"""
    print_banner()
    
    # Check Python version
    check_python_version()
    
    # Check dependencies
    check_dependencies()
    
    # Check network
    local_ip = check_network()
    
    # Show Android instructions
    show_android_instructions(local_ip)
    
    # Ask user if they want to start the cursor controller
    print("\n" + "=" * 60)
    response = input("Do you want to start the cursor controller now? (y/n): ").lower().strip()
    
    if response in ['y', 'yes']:
        start_cursor_controller()
    else:
        print("\nTo start manually later, run:")
        print(f"   python pc_receiver/cursor_controller.py")
        print(f"   # or")
        print(f"   phone2pc-cursor")
        print(f"\nYour PC's IP address is: {local_ip}")
        print("\nGood luck with your Phone2PC setup!")

if __name__ == "__main__":
    main() 