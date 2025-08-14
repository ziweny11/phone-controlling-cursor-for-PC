package com.phone2pc.cursorcontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView cameraPreview;
    private TextView statusText;
    private TextView motionDataText;
    private EditText ipAddressInput;
    private Button connectButton;
    private Button startTrackingButton;
    private Button stopTrackingButton;

    private ExecutorService cameraExecutor;
    private MotionTracker motionTracker;
    private UDPSender udpSender;
    private boolean isConnected = false;
    private boolean isTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Check permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initializeViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        statusText = findViewById(R.id.status_text);
        motionDataText = findViewById(R.id.motion_data);
        ipAddressInput = findViewById(R.id.ip_address_input);
        connectButton = findViewById(R.id.connect_button);
        startTrackingButton = findViewById(R.id.start_tracking_button);
        stopTrackingButton = findViewById(R.id.stop_tracking_button);
    }

    private void setupClickListeners() {
        connectButton.setOnClickListener(v -> toggleConnection());
        startTrackingButton.setOnClickListener(v -> startTracking());
        stopTrackingButton.setOnClickListener(v -> stopTracking());
    }

    private void toggleConnection() {
        if (!isConnected) {
            String ipAddress = ipAddressInput.getText().toString().trim();
            if (ipAddress.isEmpty()) {
                Toast.makeText(this, "Please enter PC IP address", Toast.LENGTH_SHORT).show();
                return;
            }
            connectToPC(ipAddress);
        } else {
            disconnectFromPC();
        }
    }

    private void connectToPC(String ipAddress) {
        try {
            udpSender = new UDPSender(ipAddress, 5000);
            udpSender.start();
            isConnected = true;
            updateUI();
            Toast.makeText(this, "Connected to PC", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to PC", e);
            Toast.makeText(this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectFromPC() {
        if (udpSender != null) {
            udpSender.stop();
            udpSender = null;
        }
        if (isTracking) {
            stopTracking();
        }
        isConnected = false;
        updateUI();
        Toast.makeText(this, "Disconnected from PC", Toast.LENGTH_SHORT).show();
    }

    private void startTracking() {
        if (!isConnected) {
            Toast.makeText(this, "Please connect to PC first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (motionTracker == null) {
            motionTracker = new MotionTracker(cameraPreview, udpSender);
        }
        
        motionTracker.startTracking();
        isTracking = true;
        updateUI();
        Toast.makeText(this, "Motion tracking started", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        if (motionTracker != null) {
            motionTracker.stopTracking();
        }
        isTracking = false;
        updateUI();
        Toast.makeText(this, "Motion tracking stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        runOnUiThread(() -> {
            if (isConnected) {
                statusText.setText(R.string.status_connected);
                connectButton.setText(R.string.disconnect);
                startTrackingButton.setEnabled(true);
                stopTrackingButton.setEnabled(isTracking);
            } else {
                statusText.setText(R.string.status_disconnected);
                connectButton.setText(R.string.connect);
                startTrackingButton.setEnabled(false);
                stopTrackingButton.setEnabled(false);
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build();

        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (motionTracker != null) {
            motionTracker.stopTracking();
        }
        if (udpSender != null) {
            udpSender.stop();
        }
        cameraExecutor.shutdown();
    }
} 