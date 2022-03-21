package com.sunaada.hebbar.assignment1;

import static android.content.DialogInterface.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class AppUtility {
    public static void handleException(Exception e, AppCompatActivity activity) {
        e.printStackTrace();
        createExitAlertDialogWithConsentAndExit(
                activity,
                activity.getString(crash_dialog_title),
                activity.getString(crash_dialog_message),
                activity.getString(alert_dialog_ok));
    }

    public static void createExitAlertDialogWithConsentAndExit(AppCompatActivity activity,
                                                               String title,
                                                               String message,
                                                               String positiveButtonText) {
        createAlertDialogAndShow(activity,
                title,
                message,
                positiveButtonText,
                (dialog, which) -> exitApplication(activity),
                activity.getString(empty_string),
                (dialog, which) -> { });
    }

    public static void createExitAlertDialogWithConsentAndExit(AppCompatActivity activity,
                                                               String title,
                                                               String message,
                                                               String positiveButtonText,
                                                               String negativeButtonText) {
        createAlertDialogAndShow(activity,
                title,
                message,
                positiveButtonText,
                (dialog, which) -> exitApplication(activity),
                negativeButtonText,
                (dialog, which) -> dialog.cancel());
    }

    public static void createAlertDialogAndShow(AppCompatActivity activity,
                                                String title,
                                                String message,
                                                String positiveButtonText,
                                                OnClickListener positiveButtonOnClickCallBack,
                                                String negativeButtonText,
                                                OnClickListener negativeButtonOnClickCallBack) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(positiveButtonText, positiveButtonOnClickCallBack);
        if(!negativeButtonText.isEmpty()) {
            dialogBuilder.setNegativeButton(negativeButtonText, negativeButtonOnClickCallBack);
        }

        AlertDialog dialog =  dialogBuilder.create();
        dialog.show();
    }

    public static void exitApplication(AppCompatActivity activity) {
        activity.finishAndRemoveTask();
        System.exit(0);
    }

    public static void registerButtonOnClickCallBack(AppCompatActivity activity,
                                                     View.OnClickListener listener,
                                                     ArrayList<Integer> buttonIds) {
        for(Integer id: buttonIds) {
            Button button = activity.findViewById(id);
            if(button != null) {
                button.setOnClickListener(listener);
            } else {
                // log the error for debugging purpose
                Log.d("Button not found",
                        "Button ID: " + id + " failed to register OnClick call back");
            }
        }
    }

    public static void createAndDisplayToast(AppCompatActivity activity,
                                             String message,
                                             int duration) {
        Toast toast = Toast.makeText(activity, message, duration);
        toast.show();
    }

    public static void createAndDisplayToast(AppCompatActivity activity, String message) {
        createAndDisplayToast(activity, message, Toast.LENGTH_SHORT);
    }

    public static void createAndDisplayToast(Context context, String message, int duration) {
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    public static void createAndDisplayToast(Context context, String message) {
        createAndDisplayToast(context, message, Toast.LENGTH_SHORT);
    }

    public static Executor getExecutor(AppCompatActivity activity) {
        return ContextCompat.getMainExecutor(activity);
    }

    public static NumberFormat getDefaultNumberFormat() {
        Locale loc = Locale.getDefault();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(loc);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(4);
        numberFormat.setRoundingMode(RoundingMode.HALF_UP);
        return numberFormat;
    }

    public static Float computeHeartRate(android.content.Context context, File videoFile, long offset_milliseconds) {
        if(!videoFile.exists()) {
            Log.d("Video File Missing",
                    "The video file does not exist or the video file path is invalid");
            return 0f;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoFile.getAbsolutePath());
        Uri videoFileUri = Uri.fromFile(videoFile);
        MediaPlayer mediaPlayer = MediaPlayer.create(context, videoFileUri);

        // values for computation
        // total duration of actual captured video
        final float totalDuration_ms = mediaPlayer.getDuration();
        // multiplier to convert seconds to microseconds
        final float microSecondsMultiplier = 1000000f;
        // multiplier to convert seconds to milliseconds
        final float milliSecondsMultiplier = 1000f;
        // for start time and end time discard first and last few seconds as provided by the
        // offset_milliseconds since it can have noise and disturbances due to the movement finger
        // and auto focus feature of camera
        // start time of the portion of the video considered for processing
        final float startTime_us = offset_milliseconds * milliSecondsMultiplier;
        // end time of the portion of the video considered for processing
        final float endTime_us = (totalDuration_ms - offset_milliseconds) * milliSecondsMultiplier;
        // frames per second of captured video
        final float fps = 25f;
        // total time span of the portion of the video considered for processing
        final float timeSpan_s = (endTime_us - startTime_us)/microSecondsMultiplier;
        // total number of frames considered for processing
        final float numberOfFrames = fps * timeSpan_s;
        // time increments at which frames are sampled
        final float sampleIncrement_us = microSecondsMultiplier/fps;
        // multiplier to convert heart rate per 1 minute
        final float heartRateMultiplier = timeSpan_s == 0? 0f : 60f/timeSpan_s;

        // 2D bitmap image subsampling parameters
        final int width = 100, height = 100, xSpacing = 10, ySpacing = 19;
        float i = startTime_us, j=0;
        List<Float> averageRedPixels = new ArrayList<>();
        while(j < numberOfFrames && i < endTime_us) {
            Bitmap bitmap = retriever.getFrameAtTime((int)i, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            float redPixelsCumulative = 0f;
            // row of 2D bitmap image
            for(int x=0; x<width * xSpacing; x+=xSpacing) {
                // column of 2D bitmap image
                for(int y=0; y<height * ySpacing; y+=ySpacing) {
                    int pixel = bitmap.getPixel(x, y);
                    // to calculate weighted average of red, green, and blue pixel values with
                    // ratio 5:1:1, add all pixel values with the respective multipliers and divide
                    // by the 7
                    redPixelsCumulative += (((pixel >> 16) & 0xFF) * 5 + (pixel >> 8) & 0xFF + pixel & 0xFF)/7.0f;
                }
            }

            // divide the sum of all pixel values with the total number of pixels considered
            averageRedPixels.add(redPixelsCumulative/(height * width));
            i += sampleIncrement_us;
            j++;
        }

        final float threshold = 0.0001f;
        int heartRate = 0;
        Float prevValue = averageRedPixels.get(0);
        for(int k=1; k<averageRedPixels.size(); k++) {
            if(Math.abs(prevValue - averageRedPixels.get(k)) > threshold) {
                heartRate++;
                prevValue = averageRedPixels.get(k);
            }
        }

        mediaPlayer.release();
        retriever.release();
        return heartRate * heartRateMultiplier;
    }

    public static Float computeRespiratoryRate(AccelerometerData accelerometerData,
                                               long accelerometerDataCaptureDuration_ms,
                                               long measurementOffsetTime_ms) {
        float result = Float.NaN;
        final int ratio_x = 1, ratio_y = 1, ratio_z = 5;
        float[] processedAxisValues;
        if(accelerometerData.isDataValid()) {
            processedAxisValues = accelerometerData.getWeightedAverageAxisValues(ratio_x, ratio_y, ratio_z);
            Log.d("Avg Accel Data", Arrays.toString(processedAxisValues));
            processedAxisValues = applyMovingAverageWindowFilter(processedAxisValues);
            Log.d("Smooth Accel Data", Arrays.toString(processedAxisValues));
            result = countPeaks(processedAxisValues) * 60000f /(accelerometerDataCaptureDuration_ms - 2 * measurementOffsetTime_ms);
        }

        return result;
    }

    private static float getMovingAverageScaledValue(float actual, float movingAverage) {
        final float alpha = 0.65f;
        return (float)(Math.exp(alpha) * actual + Math.exp(1-alpha) * movingAverage);
    }

    private static float[] applyMovingAverageWindowFilter(float[] values) {
        int windowSize = 20;
        float[] result = new float[values.length - 2 * (windowSize/2)];
        float sum = 0f;
        for(int i=0; i<windowSize; i++) {
            sum += values[i];
        }

        for(int i=windowSize/2; i<values.length - windowSize/2; i++) {
            result[i - windowSize/2] = getMovingAverageScaledValue(result[i - windowSize/2],sum/windowSize);
            sum -= values[i - windowSize/2];
            sum += values[i + windowSize/2];
        }

        return result;
    }

    private static int countPeaks(float[] a){
        int num;
        int prevPeak = 0;
        int curPeak = 1;
        boolean goingUp = true;
        float threshold = 0.2f * (findMax(a) - findMin(a));
        ArrayList<Integer> peaks = new ArrayList<>();
        while(curPeak < a.length) {
            if (a[curPeak] > a[curPeak - 1]) {
                if(!goingUp){
                    prevPeak = curPeak - 1;
                    goingUp = true;
                }
            }
            else {
                if(goingUp && (a[curPeak-1] - a[prevPeak] > threshold)){
                    peaks.add(curPeak - 1);
                    goingUp = false;
                }

                prevPeak = curPeak;
            }

            curPeak++;
        }

        num = peaks.size();
        Log.d("Peaks Found", Arrays.toString(peaks.toArray()));
        return num;
    }

    private static float findMin(float[] a){
        if(a.length == 0) {
            return -1f;
        }

        float min = a[0];
        int i = 1;
        while(i < a.length){
            if(a[i] < min){
                min = a[i];
            }

            i++;
        }

        return min;
    }

    private static float findMax(float[] a){
        if(a.length == 0) {
            return -1f;
        }

        float max = a[0];
        int i = 1;
        while(i < a.length){
            if(a[i] > max){
                max = a[i];
            }
            i++;
        }

        return max;
    }
}
