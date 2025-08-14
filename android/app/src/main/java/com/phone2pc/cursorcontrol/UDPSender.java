package com.phone2pc.cursorcontrol;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UDPSender {
    private static final String TAG = "UDPSender";
    private static final int UDP_PORT = 5000;
    private static final int HEARTBEAT_INTERVAL_MS = 1000; // 1 second heartbeat
    
    private String targetIP;
    private int targetPort;
    private DatagramSocket socket;
    private InetAddress targetAddress;
    private ScheduledExecutorService heartbeatExecutor;
    private boolean isRunning = false;
    
    public UDPSender(String targetIP, int targetPort) throws UnknownHostException, SocketException {
        this.targetIP = targetIP;
        this.targetPort = targetPort;
        this.targetAddress = InetAddress.getByName(targetIP);
        this.socket = new DatagramSocket();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        
        Log.d(TAG, "UDPSender initialized for " + targetIP + ":" + targetPort);
    }
    
    public void start() {
        if (isRunning) return;
        
        isRunning = true;
        Log.d(TAG, "Starting UDP sender");
        
        // Start heartbeat to keep connection alive
        startHeartbeat();
        
        // Send initial connection message
        sendConnectionMessage();
    }
    
    public void stop() {
        if (!isRunning) return;
        
        isRunning = false;
        Log.d(TAG, "Stopping UDP sender");
        
        // Stop heartbeat
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
        }
        
        // Send disconnect message
        sendDisconnectMessage();
        
        // Close socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (isRunning) {
                sendHeartbeat();
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void sendHeartbeat() {
        try {
            String heartbeat = "HEARTBEAT:" + System.currentTimeMillis();
            sendData(heartbeat.getBytes());
        } catch (Exception e) {
            Log.w(TAG, "Failed to send heartbeat", e);
        }
    }
    
    private void sendConnectionMessage() {
        try {
            String connectMsg = "CONNECT:Android_Phone2PC";
            sendData(connectMsg.getBytes());
            Log.d(TAG, "Sent connection message");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send connection message", e);
        }
    }
    
    private void sendDisconnectMessage() {
        try {
            String disconnectMsg = "DISCONNECT:Android_Phone2PC";
            sendData(disconnectMsg.getBytes());
            Log.d(TAG, "Sent disconnect message");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send disconnect message", e);
        }
    }
    
    public void sendData(byte[] data) throws IOException {
        if (!isRunning || socket == null || socket.isClosed()) {
            throw new IOException("UDPSender is not running or socket is closed");
        }
        
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            socket.send(packet);
            
            // Log data size for debugging
            if (data.length > 20) { // Don't log large data packets
                Log.d(TAG, "Sent " + data.length + " bytes to " + targetIP + ":" + targetPort);
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error sending UDP packet", e);
            throw e;
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public String getTargetIP() {
        return targetIP;
    }
    
    public int getTargetPort() {
        return targetPort;
    }
    
    public void setTargetIP(String newIP) throws UnknownHostException {
        this.targetIP = newIP;
        this.targetAddress = InetAddress.getByName(newIP);
        Log.d(TAG, "Target IP updated to: " + newIP);
    }
    
    public void setTargetPort(int newPort) {
        this.targetPort = newPort;
        Log.d(TAG, "Target port updated to: " + newPort);
    }
} 