package com.sunaada.hebbar.assignment1;

import static android.content.DialogInterface.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

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

    public static Float computeHeartRate(android.content.Context context, File videoFile) {
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
        // discard first and last 5 seconds since it can have noise and disturbances due to
        // the movement finger and auto focus feature of camera
        final float timeOffset_s = 5f;
        // total duration of actual captured video
        final float totalDuration_ms = mediaPlayer.getDuration();
        // multiplier to convert seconds to microseconds
        final float microSecondsMultiplier = 1000000f;
        // multiplier to convert seconds to milliseconds
        final float milliSecondsMultiplier = 1000f;
        // start time of the portion of the video considered for processing
        final float startTime_us = timeOffset_s * microSecondsMultiplier;
        // end time of the portion of the video considered for processing
        final float endTime_us = (totalDuration_ms/milliSecondsMultiplier - timeOffset_s) * microSecondsMultiplier;
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

        final float threshold = 0.05f;
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
}
