#!/usr/bin/env python3
"""
Phone2PC Cursor Controller
Receives motion data from Android phone via UDP and controls PC mouse cursor
"""

import socket
import threading
import time
import json
import logging
from typing import Tuple, Optional
import pyautogui
import psutil

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class CursorController:
    def __init__(self, host: str = '0.0.0.0', port: int = 5000):
        self.host = host
        self.port = port
        self.socket = None
        self.is_running = False
        self.connected_clients = set()
        
        # Cursor control parameters
        self.sensitivity = 1.0
        self.smoothing_factor = 0.7
        self.smoothed_dx = 0.0
        self.smoothed_dy = 0.0
        
        # Performance monitoring
        self.last_motion_time = time.time()
        self.motion_count = 0
        self.avg_latency = 0.0
        
        # Disable pyautogui failsafe for better performance
        pyautogui.FAILSAFE = False
        
        logger.info("Cursor Controller initialized")
    
    def start(self):
        """Start the UDP server and cursor control"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.socket.bind((self.host, self.port))
            self.socket.settimeout(1.0)  # 1 second timeout for non-blocking operation
            
            self.is_running = True
            logger.info(f"UDP server started on {self.host}:{self.port}")
            
            # Start listening thread
            listen_thread = threading.Thread(target=self._listen_loop, daemon=True)
            listen_thread.start()
            
            # Start status monitoring thread
            monitor_thread = threading.Thread(target=self._status_monitor, daemon=True)
            monitor_thread.start()
            
            logger.info("Cursor Controller started successfully")
            logger.info("Move your phone to control the cursor. Press Ctrl+C to stop.")
            
            # Keep main thread alive
            try:
                while self.is_running:
                    time.sleep(0.1)
            except KeyboardInterrupt:
                logger.info("Shutdown requested by user")
                
        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            self.stop()
    
    def stop(self):
        """Stop the server and cleanup"""
        self.is_running = False
        if self.socket:
            self.socket.close()
        logger.info("Cursor Controller stopped")
    
    def _listen_loop(self):
        """Main listening loop for UDP packets"""
        while self.is_running:
            try:
                data, addr = self.socket.recvfrom(1024)
                if data:
                    self._process_packet(data, addr)
            except socket.timeout:
                continue
            except Exception as e:
                if self.is_running:
                    logger.error(f"Error in listen loop: {e}")
    
    def _process_packet(self, data: bytes, addr: Tuple[str, int]):
        """Process incoming UDP packet"""
        try:
            message = data.decode('utf-8').strip()
            client_ip = addr[0]
            
            # Update connected clients
            self.connected_clients.add(client_ip)
            
            # Process different message types
            if message.startswith('MOTION:'):
                self._handle_motion_data(message, client_ip)
            elif message.startswith('CONNECT:'):
                logger.info(f"Client connected: {client_ip} - {message}")
            elif message.startswith('DISCONNECT:'):
                logger.info(f"Client disconnected: {client_ip} - {message}")
                self.connected_clients.discard(client_ip)
            elif message.startswith('HEARTBEAT:'):
                # Heartbeat received, client is alive
                pass
            else:
                logger.warning(f"Unknown message format from {client_ip}: {message}")
                
        except Exception as e:
            logger.error(f"Error processing packet from {addr}: {e}")
    
    def _handle_motion_data(self, message: str, client_ip: str):
        """Handle motion data and update cursor position"""
        try:
            # Parse motion data: "MOTION:dx,dy"
            motion_part = message.split(':', 1)[1]
            dx_str, dy_str = motion_part.split(',')
            
            dx = float(dx_str)
            dy = float(dy_str)
            
            # Apply sensitivity
            dx *= self.sensitivity
            dy *= self.sensitivity
            
            # Apply smoothing
            self.smoothed_dx = (self.smoothing_factor * self.smoothed_dx + 
                              (1 - self.smoothing_factor) * dx)
            self.smoothed_dy = (self.smoothing_factor * self.smoothed_dy + 
                              (1 - self.smoothing_factor) * dy)
            
            # Move cursor
            self._move_cursor(self.smoothed_dx, self.smoothed_dy)
            
            # Update performance metrics
            current_time = time.time()
            self.motion_count += 1
            if self.motion_count > 1:
                latency = current_time - self.last_motion_time
                self.avg_latency = (self.avg_latency * 0.9 + latency * 0.1)
            
            self.last_motion_time = current_time
            
        except (ValueError, IndexError) as e:
            logger.error(f"Invalid motion data format from {client_ip}: {message}")
        except Exception as e:
            logger.error(f"Error handling motion data: {e}")
    
    def _move_cursor(self, dx: float, dy: float):
        """Move the cursor by the specified delta"""
        try:
            # Get current cursor position
            current_x, current_y = pyautogui.position()
            
            # Calculate new position
            new_x = int(current_x + dx)
            new_y = int(current_y + dy)
            
            # Ensure cursor stays within screen bounds
            screen_width, screen_height = pyautogui.size()
            new_x = max(0, min(new_x, screen_width - 1))
            new_y = max(0, min(new_y, screen_height - 1))
            
            # Move cursor
            pyautogui.moveTo(new_x, new_y, duration=0)
            
        except Exception as e:
            logger.error(f"Error moving cursor: {e}")
    
    def _status_monitor(self):
        """Monitor and log system status"""
        while self.is_running:
            try:
                time.sleep(5)  # Update every 5 seconds
                
                if self.connected_clients:
                    # Log performance metrics
                    fps = self.motion_count / 5.0 if self.motion_count > 0 else 0
                    latency_ms = self.avg_latency * 1000
                    
                    logger.info(f"Status: {len(self.connected_clients)} client(s) connected, "
                              f"FPS: {fps:.1f}, Latency: {latency_ms:.1f}ms")
                    
                    # Reset counters
                    self.motion_count = 0
                else:
                    logger.info("Status: No clients connected")
                    
            except Exception as e:
                logger.error(f"Error in status monitor: {e}")
    
    def set_sensitivity(self, sensitivity: float):
        """Set cursor sensitivity multiplier"""
        self.sensitivity = max(0.1, min(5.0, sensitivity))
        logger.info(f"Sensitivity set to: {self.sensitivity}")
    
    def set_smoothing(self, smoothing: float):
        """Set motion smoothing factor (0.0 = no smoothing, 1.0 = maximum smoothing)"""
        self.smoothing_factor = max(0.0, min(1.0, smoothing))
        logger.info(f"Smoothing factor set to: {self.smoothing_factor}")
    
    def get_status(self) -> dict:
        """Get current status information"""
        return {
            'connected_clients': len(self.connected_clients),
            'client_ips': list(self.connected_clients),
            'sensitivity': self.sensitivity,
            'smoothing_factor': self.smoothing_factor,
            'avg_latency_ms': self.avg_latency * 1000,
            'is_running': self.is_running
        }


def main():
    """Main function to run the cursor controller"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Phone2PC Cursor Controller')
    parser.add_argument('--host', default='0.0.0.0', help='Host to bind to (default: 0.0.0.0)')
    parser.add_argument('--port', type=int, default=5000, help='Port to listen on (default: 5000)')
    parser.add_argument('--sensitivity', type=float, default=1.0, help='Cursor sensitivity (default: 1.0)')
    parser.add_argument('--smoothing', type=float, default=0.7, help='Motion smoothing (0.0-1.0, default: 0.7)')
    
    args = parser.parse_args()
    
    # Create and configure controller
    controller = CursorController(host=args.host, port=args.port)
    controller.set_sensitivity(args.sensitivity)
    controller.set_smoothing(args.smoothing)
    
    try:
        controller.start()
    except KeyboardInterrupt:
        logger.info("Shutdown requested")
    finally:
        controller.stop()


if __name__ == "__main__":
    main() 