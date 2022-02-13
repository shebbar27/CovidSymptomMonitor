package com.sunaada.hebbar.assignment1;

import static com.sunaada.hebbar.assignment1.AppUtility.*;
import static com.sunaada.hebbar.assignment1.R.id.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private static final int REQUEST_PERMISSIONS_CODE = 27;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean isCameraInitialized = false;
    private boolean isCameraPermitted = false;
    private VideoCapture videoCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppUtility.registerButtonOnClickCallBack(this,
                this,
                new ArrayList<Integer>() {{
                    add(measure_heart_rate_button);
                    add(measure_respiratory_rate_button);
                    add(upload_signs_button);
                    add(R.id.symptoms_button);
                    add(R.id.exit_button);
                }}
        );

        this.initializeCamera();
        this.startCamera();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case measure_heart_rate_button:
                this.measureHeartRate();
                break;
            case measure_respiratory_rate_button:
                this.measureRespiratoryRate();
                break;
            case upload_signs_button:
                this.uploadSignsData();
                break;
            case symptoms_button:
                this.switchToSymptomLoggingActivity();
                break;
            case exit_button:
                this.exitApplication();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSIONS_CODE && grantResults.length > 0) {
            if(this.isCameraPermissionsGranted()) {
                this.isCameraPermitted = true;
                this.configureAndStartPreviewCamera();
            }
            else {
                ((ActivityManager)(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }
        }
    }

    private void measureHeartRate() {
        // TODO
    }

    private void measureRespiratoryRate() {
        // TODO
    }

    private void uploadSignsData() {
        createAndDisplayToast(this,
                getString(uploading_measured_signs_data),
                Toast.LENGTH_LONG);
        // TODO
        createAndDisplayToast(this,
                getString(uploading_measured_signs_data_success));
    }

    private void switchToSymptomLoggingActivity() {
        try {
            Intent intent = new Intent(this, SymptomLoggingActivity.class);
            this.startActivity(intent);
        }
        catch (Exception e) {
            handleException(e, this);
        }
    }

    private void exitApplication() {
        createExitAlertDialogWithConsentAndExit(this,
                getString(exit_dialog_title),
                getString(exit_dialog_message),
                getString(alert_dialog_yes),
                getString(alert_dialog_no));
    }

    private boolean isCameraPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("RestrictedApi")
    private void initializeCamera() {
        this.previewView = findViewById(rear_camera_view);
        this.previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        this.previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        this.videoCapture = new VideoCapture.Builder().build();
        this.isCameraInitialized = true;
    }

    private void startCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions( new String[]{ CAMERA_PERMISSION }, REQUEST_PERMISSIONS_CODE);
        }
        else {
            this.configureAndStartPreviewCamera();
        }
    }

    private void configureAndStartPreviewCamera() {
        if(!this.isCameraPermitted) {
            Log.d("Camera_Permit_Denied", "Camera permission denied!");
            return;
        }
        else if(!this.isCameraInitialized) {
            Log.d("Camera_Not_Initialized", "Camera was not initialized!");
            return;
        }

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
                e.printStackTrace();
                handleException(e, this);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, this.videoCapture);
    }
}