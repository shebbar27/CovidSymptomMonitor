package com.sunaada.hebbar.assignment1;

import static android.content.DialogInterface.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

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

    public static Float computeHeartRate(File videoFile) {
        // TODO
        return 60f;
    }
}
