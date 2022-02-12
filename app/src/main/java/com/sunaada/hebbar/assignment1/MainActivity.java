package com.sunaada.hebbar.assignment1;

import static com.sunaada.hebbar.assignment1.AppUtility.*;
import static com.sunaada.hebbar.assignment1.R.id.*;
import static com.sunaada.hebbar.assignment1.R.string.*;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnClickListener {

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
        createExitAlertDialogWithConsent(this,
                getString(exit_dialog_title),
                getString(exit_dialog_message),
                getString(alert_dialog_yes),
                getString(alert_dialog_no));
    }
}