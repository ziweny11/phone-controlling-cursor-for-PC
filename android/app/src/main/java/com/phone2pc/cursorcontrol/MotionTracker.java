package com.phone2pc.cursorcontrol;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import androidx.camera.view.PreviewView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MotionTracker {
    private static final String TAG = "MotionTracker";
    private static final int TRACKING_INTERVAL_MS = 50; // 20 FPS for low latency
    private static final double MOTION_THRESHOLD = 2.0; // Minimum motion to trigger cursor movement
    private static final double SENSITIVITY_MULTIPLIER = 2.0; // Adjust cursor sensitivity

    private PreviewView cameraPreview;
    private UDPSender udpSender;
    private ScheduledExecutorService trackingExecutor;
    private boolean isTracking = false;

    // OpenCV variables for optical flow
    private Mat prevGray;
    private Mat currGray;
    private MatOfPoint2f prevPts;
    private MatOfPoint2f currPts;
    private Mat status;
    private Mat err;
    private Point[] prevPoints;
    private Point[] currPoints;

    // Motion smoothing
    private double smoothedDx = 0;
    private double smoothedDy = 0;
    private static final double SMOOTHING_FACTOR = 0.7;

    public MotionTracker(PreviewView cameraPreview, UDPSender udpSender) {
        this.cameraPreview = cameraPreview;
        this.udpSender = udpSender;
        this.trackingExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Initialize OpenCV matrices
        prevGray = new Mat();
        currGray = new Mat();
        prevPts = new MatOfPoint2f();
        currPts = new MatOfPoint2f();
        status = new Mat();
        err = new Mat();
    }

    public void startTracking() {
        if (isTracking) return;
        
        isTracking = true;
        Log.d(TAG, "Starting motion tracking");
        
        // Start periodic tracking
        trackingExecutor.scheduleAtFixedRate(
            this::processFrame,
            0,
            TRACKING_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    public void stopTracking() {
        if (!isTracking) return;
        
        isTracking = false;
        Log.d(TAG, "Stopping motion tracking");
        
        // Stop tracking executor
        if (trackingExecutor != null && !trackingExecutor.isShutdown()) {
            trackingExecutor.shutdown();
        }
    }

    private void processFrame() {
        try {
            // Capture current frame from camera preview
            Bitmap currentBitmap = captureFrame();
            if (currentBitmap == null) return;

            // Convert to OpenCV Mat
            Mat currentFrame = new Mat();
            Utils.bitmapToMat(currentBitmap, currentFrame);
            
            // Convert to grayscale for optical flow
            Mat grayFrame = new Mat();
            Imgproc.cvtColor(currentFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            
            // Process motion detection
            detectMotion(grayFrame);
            
            // Clean up
            currentFrame.release();
            grayFrame.release();
            currentBitmap.recycle();
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }
    }

    private Bitmap captureFrame() {
        try {
            // Create a bitmap from the camera preview
            View view = cameraPreview;
            if (view.getWidth() == 0 || view.getHeight() == 0) return null;
            
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturing frame", e);
            return null;
        }
    }

    private void detectMotion(Mat grayFrame) {
        if (prevGray.empty()) {
            // First frame - initialize
            grayFrame.copyTo(prevGray);
            initializeTrackingPoints(grayFrame);
            return;
        }

        // Copy current frame to previous
        grayFrame.copyTo(currGray);

        // Calculate optical flow using Lucas-Kanade method
        Video.calcOpticalFlowPyrLK(prevGray, currGray, prevPts, currPts, status, err);

        // Process optical flow results
        processOpticalFlow();

        // Update previous frame and points
        currGray.copyTo(prevGray);
        currPts.copyTo(prevPts);
    }

    private void initializeTrackingPoints(Mat frame) {
        // Create a grid of tracking points across the frame
        int rows = frame.rows();
        int cols = frame.cols();
        int gridSize = 20;
        
        Point[] points = new Point[gridSize * gridSize];
        int index = 0;
        
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int x = (int) (cols * (j + 0.5) / gridSize);
                int y = (int) (rows * (i + 0.5) / gridSize);
                points[index++] = new Point(x, y);
            }
        }
        
        prevPoints = points;
        currPoints = new Point[points.length];
        
        // Convert to MatOfPoint2f
        prevPts.fromArray(points);
        currPts.fromArray(points);
    }

    private void processOpticalFlow() {
        if (prevPoints == null || currPoints == null) return;

        double totalDx = 0;
        double totalDy = 0;
        int validPoints = 0;

        // Calculate average motion from all tracking points
        for (int i = 0; i < prevPoints.length; i++) {
            if (status.get(i, 0)[0] == 1) { // Point was tracked successfully
                double dx = currPoints[i].x - prevPoints[i].x;
                double dy = currPoints[i].y - prevPoints[i].y;
                
                // Filter out outliers (excessive motion)
                if (Math.abs(dx) < 50 && Math.abs(dy) < 50) {
                    totalDx += dx;
                    totalDy += dy;
                    validPoints++;
                }
            }
        }

        if (validPoints > 0) {
            // Calculate average motion
            double avgDx = totalDx / validPoints;
            double avgDy = totalDy / validPoints;

            // Apply smoothing
            smoothedDx = SMOOTHING_FACTOR * smoothedDx + (1 - SMOOTHING_FACTOR) * avgDx;
            smoothedDy = SMOOTHING_FACTOR * smoothedDy + (1 - SMOOTHING_FACTOR) * avgDy;

            // Apply sensitivity and threshold
            double adjustedDx = smoothedDx * SENSITIVITY_MULTIPLIER;
            double adjustedDy = smoothedDy * SENSITIVITY_MULTIPLIER;

            // Only send if motion exceeds threshold
            if (Math.abs(adjustedDx) > MOTION_THRESHOLD || Math.abs(adjustedDy) > MOTION_THRESHOLD) {
                sendMotionData(adjustedDx, adjustedDy);
                Log.d(TAG, String.format("Motion: (%.2f, %.2f)", adjustedDx, adjustedDy));
            }
        }
    }

    private void sendMotionData(double dx, double dy) {
        if (udpSender != null && udpSender.isRunning()) {
            try {
                // Create motion data packet
                String motionData = String.format("MOTION:%.2f,%.2f", dx, dy);
                udpSender.sendData(motionData.getBytes());
            } catch (Exception e) {
                Log.e(TAG, "Error sending motion data", e);
            }
        }
    }

    public void cleanup() {
        stopTracking();
        
        // Release OpenCV resources
        if (prevGray != null) prevGray.release();
        if (currGray != null) currGray.release();
        if (prevPts != null) prevPts.release();
        if (currPts != null) currPts.release();
        if (status != null) status.release();
        if (err != null) err.release();
    }
} 