package com.sunaada.hebbar.assignment1;

import static com.sunaada.hebbar.assignment1.AppUtility.*;
import static com.sunaada.hebbar.assignment1.R.id.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import net.sqlcipher.database.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements OnClickListener, VideoCapture.OnVideoSavedCallback {

    private static final int REQUEST_PERMISSIONS_CODE = 27;
    private static final String FINGERTIP_VIDEO_FILENAME = "FingerTipVideo";
    private static final long VIDEO_RECORDING_DURATION_MILLISECONDS = 46000;
    private static final long ACCELEROMETER_DATA_CAPTURE_DURATION_MILLISECONDS = 45000;
    private static final long VIDEO_OFFSET_TIME_MILLISECONDS = 12000;
    private static final long ACCELEROMETER_OFFSET_TIME_MILLISECONDS = 7000;
    private static final long VIBRATION_DURATION_SHORT = 250;
    private static final long VIBRATION_DURATION_LONG = 500;
    private static final int BROADCAST_REQUEST_CODE = 127;
    private static final int ALARM_BROADCAST_INTERVAL_MILLISECONDS = 15 * 60 * 1000;
    private static final NumberFormat DEFAULT_NUMBER_FORMAT = AppUtility.getDefaultNumberFormat();
    private static final ArrayList<String> PERMISSIONS = new ArrayList<>(
            Arrays.asList(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION)
    );
    private static File videoCaptureFile;

    private PreviewView previewView;
    private VideoView videoView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private VideoCapture videoCapture;
    private Camera camera;
    private TextView heartRateTextView;
    private TextView respiratoryRateTextView;
    private ExecutorService executorService;
    private Vibrator vibrator;
    private Float heartRate = 0f;
    private Float respiratoryRate = 0f;
    private boolean isCameraConfigured = false;
    private boolean heartRateMeasurementInProgress = false;
    private boolean respiratoryRateMeasurementInProgress = false;
    private boolean isSignsDataUploaded = false;
    private long latestRecordID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            PERMISSIONS.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        AppUtility.registerButtonOnClickCallBack(this,
                this,
                new ArrayList<Integer>() {{
                    add(measure_heart_rate_button);
                    add(measure_respiratory_rate_button);
                    add(upload_signs_button);
                    add(R.id.symptoms_button);
                }}
        );

        videoCaptureFile = new File(getApplicationContext().getFilesDir(), FINGERTIP_VIDEO_FILENAME);
        this.heartRateTextView = findViewById(heart_rate_textview);
        this.respiratoryRateTextView = findViewById(respiratory_rate_textview);
        this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        this.executorService = Executors.newCachedThreadPool();
        this.initializeCamera();
        this.getPermissions();
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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE && grantResults.length > 0 && this.areAllPermissionsGranted()) {
            this.configureAndStartCameraPreview();
            this.initializeAndStartGpsLocationService();
        } else {
            createExitAlertDialogWithConsentAndExit(this,
                    getString(camera_permission_denied_alert_title),
                    getString(camera_permission_denied_alert_message),
                    getString(alert_dialog_ok));
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
        Log.i("Video Saved",
                "Video has been saved at: " + videoCaptureFile);
        createAndDisplayToast(this,
                getString(R.string.video_saved_message) + videoCaptureFile,
                Toast.LENGTH_LONG);
        this.playbackCapturedVideo();
        Log.d("Current Thread",
                "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                        + "Current Thread: " + Thread.currentThread());
        this.computeAndUpdateHeartRate(this::updateHeartRate);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
        if (cause != null) {
            cause.printStackTrace();
        }

        Log.i("Video Capture Failed", message, cause);
        createAndDisplayToast(this,
                getString(save_video_error_message) + videoCaptureFile,
                Toast.LENGTH_LONG);
        this.heartRateMeasurementInProgress = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.executorService != null) {
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    this.executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.executorService.shutdownNow();
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putFloat("heartRate", this.heartRate);
        savedInstanceState.putFloat("respiratoryRate", this.respiratoryRate);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.heartRate = savedInstanceState.getFloat("heartRate");
        this.respiratoryRate = savedInstanceState.getFloat("respiratoryRate");
    }

    private void measureHeartRate() {
        if (this.heartRateMeasurementInProgress) {
            createAndDisplayToast(this, getString(heart_rate_measurement_in_progress));
            return;
        }

        if (this.respiratoryRateMeasurementInProgress) {
            createAndDisplayToast(this, getString(respiratory_rate_measurement_in_progress));
            return;
        }

        if (!this.isCameraConfigured) {
            this.initializeCamera();
        }

        this.heartRateMeasurementInProgress = true;
        this.heartRate = Float.NaN;
        this.updateHeartRateTextView();
        createAndDisplayToast(this,
                getString(heart_rate_measurement_started_message),
                Toast.LENGTH_LONG);
        Handler handler = new Handler();
        this.startVideoCapture();
        handler.postDelayed(this::stopVideoCapture, VIDEO_RECORDING_DURATION_MILLISECONDS);
    }

    private void measureRespiratoryRate() {
        if (this.heartRateMeasurementInProgress) {
            createAndDisplayToast(this, getString(heart_rate_measurement_in_progress));
            return;
        }

        if (this.respiratoryRateMeasurementInProgress) {
            createAndDisplayToast(this, getString(respiratory_rate_measurement_in_progress));
            return;
        }

        this.respiratoryRateMeasurementInProgress = true;
        this.respiratoryRate = Float.NaN;
        this.updateRespiratoryRateTextView();
        createAndDisplayToast(this,
                getString(respiratory_rate_measurement_started),
                Toast.LENGTH_LONG);
        this.vibrateDeviceDefault(VIBRATION_DURATION_SHORT);
        this.executorService.execute(this::readAccelerometerSensorAndUpdateRespiratoryRate);
    }

    private void uploadSignsData() {
        if (this.heartRateTextView.getText().toString().isEmpty()) {
            createAndDisplayToast(this,
                    getString(upload_signs_heart_rate_prerequisite_message),
                    Toast.LENGTH_LONG);
            return;
        }

        if (this.respiratoryRateTextView.getText().toString().isEmpty()) {
            createAndDisplayToast(this,
                    getString(upload_signs_respiratory_rate_prerequisite_message),
                    Toast.LENGTH_LONG);
            return;
        }

        if (this.heartRateMeasurementInProgress) {
            createAndDisplayToast(this, getString(heart_rate_measurement_in_progress));
            return;
        }

        if (this.respiratoryRateMeasurementInProgress) {
            createAndDisplayToast(this, getString(respiratory_rate_measurement_in_progress));
            return;
        }

        createAndDisplayToast(this,
                getString(uploading_measured_signs_data),
                Toast.LENGTH_LONG);
        this.performDataBaseUpdate();
        createAndDisplayToast(this,
                getString(uploading_measured_signs_data_success));
        this.isSignsDataUploaded = true;
    }

    private void switchToSymptomLoggingActivity() {
        if (!this.isSignsDataUploaded) {
            createAndDisplayToast(this,
                    getString(symptoms_navigation_prerequisite_message),
                    Toast.LENGTH_LONG);
            return;
        }

        try {
            Intent intent = new Intent(getApplicationContext(), SymptomLoggingActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(SymptomsDbHelper.RECORD_ID_KEY, String.valueOf(this.latestRecordID));
            //intent.putExtra(SymptomsDbHelper.RECORD_ID_KEY, this.latestRecordID);
            intent.putExtras(bundle);
            this.startActivity(intent);
        } catch (Exception e) {
            handleException(e, this);
        }
    }

    private void getPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    private boolean areAllPermissionsGranted() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    @SuppressLint("RestrictedApi")
    private void initializeCamera() {
        this.previewView = findViewById(camera_preview_view);
        this.previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        this.previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        this.videoCapture = new VideoCapture.Builder().build();
        this.videoView = findViewById(playback_video_view);
        this.videoView.setVisibility(View.INVISIBLE);
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
                .Builder(videoCaptureFile)
                .build();
    }

    @SuppressLint("RestrictedApi")
    private void startVideoCapture() {
        Log.d("Capture Started", "Video Capture Started");
        this.vibrateDeviceDefault(VIBRATION_DURATION_SHORT);
        this.camera.getCameraControl().enableTorch(true);
        VideoCapture.OutputFileOptions outputFileOptions = this.getVideoOutputFileOptions();
        this.videoCapture.startRecording(outputFileOptions, getExecutor(this), this);
    }

    @SuppressLint("RestrictedApi")
    private void stopVideoCapture() {
        this.videoCapture.stopRecording();
        Log.d("Capture Stopped", "Video Capture Stopped");
        createAndDisplayToast(this, getString(video_capture_completed_message), Toast.LENGTH_LONG);
        this.camera.getCameraControl().enableTorch(false);
    }

    private void computeAndUpdateHeartRate(final ExecutorServiceCallBackInterface<Float> callBack) {
        executorService.execute(() -> {
            Log.d("Current Thread",
                    "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                            + "Current Thread: " + Thread.currentThread());
            try {
                Float heartRate = computeHeartRate(getApplicationContext(), videoCaptureFile, VIDEO_OFFSET_TIME_MILLISECONDS);
                callBack.onExecuteTaskComplete(heartRate);
            } catch (Exception e) {
                Float errorHeartRate = Float.NaN;
                callBack.onExecuteTaskComplete(errorHeartRate);
            }
        });
    }

    private void updateHeartRateTextView() {
        this.heartRateTextView.setText(DEFAULT_NUMBER_FORMAT.format(this.heartRate));
    }

    private void updateHeartRate(Float heartRate) {
        runOnUiThread(() -> {
            Log.d("Current Thread",
                    "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                            + "Current Thread: " + Thread.currentThread());
            this.heartRate = heartRate;
            this.updateHeartRateTextView();
            createAndDisplayToast(this, getString(heart_rate_measurement_completed_message), Toast.LENGTH_LONG);
            this.heartRateMeasurementInProgress = false;
            this.vibrateDeviceDefault(VIBRATION_DURATION_LONG);
        });
    }

    private void playbackCapturedVideo() {
        Log.d("Current Thread",
                "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                        + "Current Thread: " + Thread.currentThread());
        MediaController mediaController = new MediaController(this);
        this.videoView.setMediaController(mediaController);
        this.videoView.setVisibility(View.VISIBLE);
        this.previewView.setVisibility(View.INVISIBLE);
        this.videoView.setVideoURI(Uri.parse(videoCaptureFile.toString()));
        this.videoView.start();
        this.videoView.setOnCompletionListener(mediaPlayer -> {
            this.previewView.setVisibility(View.VISIBLE);
            this.videoView.setVisibility(View.INVISIBLE);
        });
    }

    private void updateRespiratoryRateTextView() {
        this.respiratoryRateTextView.setText(DEFAULT_NUMBER_FORMAT.format(this.respiratoryRate));
    }

    private void updateRespiratoryRate(Float respiratoryRate) {
        runOnUiThread(() -> {
            Log.d("Current Thread",
                    "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                            + "Current Thread: " + Thread.currentThread());
            this.respiratoryRate = respiratoryRate;
            this.updateRespiratoryRateTextView();
            createAndDisplayToast(this, getString(respiratory_rate_measurement_completed), Toast.LENGTH_LONG);
            this.respiratoryRateMeasurementInProgress = false;
            this.vibrateDeviceDefault(VIBRATION_DURATION_LONG);
        });
    }

    private void readAccelerometerSensorAndUpdateRespiratoryRate() {
        Log.d("Accelerometer Started", "Started capturing accelerometer data");
        Log.d("Current Thread",
                "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                        + "Current Thread: " + Thread.currentThread());
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        AccelerometerData accelerometerData = new AccelerometerData();
        long accelerometerDataCaptureStartTime = System.currentTimeMillis();
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (System.currentTimeMillis() < accelerometerDataCaptureStartTime
                        + ACCELEROMETER_OFFSET_TIME_MILLISECONDS) {
                    // for start time and end time discard first and last few seconds as provided
                    // by the offset_milliseconds since it can have noise and disturbances due to
                    // the movement of phone
                    return;
                }

                Sensor changedSensor = sensorEvent.sensor;
                if (changedSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    accelerometerData.appendAxesValues(sensorEvent);
                }

                if (System.currentTimeMillis() > accelerometerDataCaptureStartTime
                        + ACCELEROMETER_DATA_CAPTURE_DURATION_MILLISECONDS
                        - ACCELEROMETER_OFFSET_TIME_MILLISECONDS) {
                    sensorManager.unregisterListener(this);
                    Log.d("Accelerometer Complete", "Capturing accelerometer data completed");
                    float measuredRespiratoryRate = computeRespiratoryRate(accelerometerData,
                            ACCELEROMETER_DATA_CAPTURE_DURATION_MILLISECONDS,
                            ACCELEROMETER_OFFSET_TIME_MILLISECONDS);
                    Log.d("Resp Measure Complete", "Respiratory Rate Measurement completed successfully");
                    updateRespiratoryRate(measuredRespiratoryRate);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // NOOP
            }
        };

        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void vibrateDeviceDefault(long duration) {
        final VibrationEffect vibrationEffect1;

        // this is the only type of the vibration which requires system version Oreo (API 26)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            // this effect creates the vibration of default amplitude for 1000ms(1 sec)
            vibrationEffect1 = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE);

            // it is safe to cancel other vibrations currently taking place
            this.vibrator.cancel();
            this.vibrator.vibrate(vibrationEffect1);
        }
    }

    private void performDataBaseUpdate() {
        SymptomsDbHelper symptomsDbHelper = new SymptomsDbHelper(getApplicationContext());
        SQLiteDatabase db = symptomsDbHelper.database;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String timeStamp = sdf.format(new Date());
        this.latestRecordID = db.insert(SymptomsDbHelper.SYMPTOMS_TABLE_NAME,
                null,
                SymptomsDbHelper.getDatabaseRowForInserting(
                        this.heartRate,
                        this.respiratoryRate,
                        timeStamp));
        db.close();
    }

    private void initializeAndStartGpsLocationService() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
                        BROADCAST_REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_MUTABLE);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                ALARM_BROADCAST_INTERVAL_MILLISECONDS,
                pendingIntent);

        AppUtility.createAndDisplayToast(this,
                String.format(Locale.US,
                        "Location data and network speed will be updated approximately every %d minutes",
                        ALARM_BROADCAST_INTERVAL_MILLISECONDS/60000),
                Toast.LENGTH_LONG);
    }
}