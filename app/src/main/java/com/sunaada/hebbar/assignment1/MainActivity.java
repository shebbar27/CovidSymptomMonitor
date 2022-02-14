package com.sunaada.hebbar.assignment1;

import static com.sunaada.hebbar.assignment1.AppUtility.*;
import static com.sunaada.hebbar.assignment1.R.id.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.io.*;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity
        implements OnClickListener, VideoCapture.OnVideoSavedCallback {

    private static final int REQUEST_PERMISSIONS_CODE = 27;
    private static final String FINGERTIP_VIDEO_FILENAME = "FingerTipVideo";
    private static final int VIDEO_RECORDING_DURATION = 45000;
    private static final NumberFormat DEFAULT_NUMBER_FORMAT = AppUtility.getDefaultNumberFormat();
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    private static String capturedVideoOutputDir;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean isCameraConfigured = false;
    private boolean heartRateMeasurementInProgress = false;
    private VideoCapture videoCapture;
    private Camera camera;
    private Float HeartRate;
    private EditText editText;

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

        capturedVideoOutputDir = getApplicationContext().getFilesDir().toString();
        this.editText = findViewById(heart_rate_textview);
        this.initializeCamera();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case measure_heart_rate_button:
                if(!heartRateMeasurementInProgress) {
                    this.measureHeartRate();
                }

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
        if(requestCode == REQUEST_PERMISSIONS_CODE && grantResults.length > 0 && this.areAllPermissionsGranted()) {
            this.configureAndStartCameraPreview();
        }
        else {
            createExitAlertDialogWithConsentAndExit(this,
                    getString(camera_permission_denied_alert_title),
                    getString(camera_permission_denied_alert_message),
                    getString(alert_dialog_ok));
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
        Log.i("Video Capture Begins",
                "Video Capture has been started! Video will be saved in: " + capturedVideoOutputDir + "/" + FINGERTIP_VIDEO_FILENAME);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
        if(cause != null) {
            cause.printStackTrace();
        }
        
        Log.i("Video Capture Failed", message, cause);
    }

    private void measureHeartRate() {
        if(isCameraConfigured) {
            this.HeartRate = 0.0f;
            this.updateHeartRateEditText();
            this.startVideoCapture();
            Handler handler = new Handler();
            handler.postDelayed(this::stopVideoCaptureAndComputeHeartRate, VIDEO_RECORDING_DURATION);
            this.updateHeartRateEditText();
        }
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

    private boolean areAllPermissionsGranted() {
        for(String PERMISSION: PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    @SuppressLint("RestrictedApi")
    private void initializeCamera() {
        this.previewView = findViewById(rear_camera_view);
        this.previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        this.previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        this.videoCapture = new VideoCapture.Builder().build();
        requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
    }

    private void configureAndStartCameraPreview() {
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
        }, getExecutor(this));

        isCameraConfigured = true;
    }

    @SuppressLint("RestrictedApi")
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
         Preview preview = new Preview.Builder()
                 .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                 .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        this.camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, this.videoCapture);
    }

    @SuppressLint("RestrictedApi")
    private VideoCapture.OutputFileOptions getVideoOutputFileOptions() {
        return new VideoCapture.OutputFileOptions
                .Builder(new File(capturedVideoOutputDir, FINGERTIP_VIDEO_FILENAME))
                .build();
    }

    @SuppressLint("RestrictedApi")
    private void startVideoCapture() {
        heartRateMeasurementInProgress = true;
        this.camera.getCameraControl().enableTorch(true);
        VideoCapture.OutputFileOptions outputFileOptions = this.getVideoOutputFileOptions();
        this.videoCapture.startRecording(outputFileOptions, getExecutor(this), this);
        Log.d("Capture Started", "Video Capture Started");
    }

    @SuppressLint("RestrictedApi")
    private void stopVideoCapture() {
        this.videoCapture.stopRecording();
        Log.d("Capture Stopped", "Video Capture Stopped");
        this.camera.getCameraControl().enableTorch(false);
        heartRateMeasurementInProgress = false;
    }

    private void stopVideoCaptureAndComputeHeartRate() {
        this.stopVideoCapture();
        this.HeartRate = computeHeartRate(new File(capturedVideoOutputDir,
                FINGERTIP_VIDEO_FILENAME));
        this.updateHeartRateEditText();
    }

    private void updateHeartRateEditText() {
        this.editText.setText(DEFAULT_NUMBER_FORMAT.format(this.HeartRate));
    }
}