package com.sunaada.hebbar.assignment1;

import static com.sunaada.hebbar.assignment1.AppUtility.createExitAlertDialogWithConsentAndExit;
import static com.sunaada.hebbar.assignment1.AppUtility.handleException;
import static com.sunaada.hebbar.assignment1.R.string.alert_dialog_no;
import static com.sunaada.hebbar.assignment1.R.string.alert_dialog_yes;
import static com.sunaada.hebbar.assignment1.R.string.exit_dialog_message;
import static com.sunaada.hebbar.assignment1.R.string.exit_dialog_title;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText usernameEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.usernameEditText = this.findViewById(R.id.username_edit_text);
        this.passwordEditText = this.findViewById(R.id.password_edit_text);
        AppUtility.registerButtonOnClickCallBack(this,
                this,
                new ArrayList<Integer>() {{
                    add(R.id.login_button);
                    add(R.id.exit_button);
                }}
        );
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.exit_button) {
            this.exitApplication();
        }
        else if(this.usernameEditText != null &&
                this.usernameEditText.getText().toString().equals(getString(R.string.username))  &&
                this.passwordEditText != null &&
                this.passwordEditText.getText().toString().equals(getString(R.string.password))) {
            try {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                this.startActivity(intent);
            }
            catch (Exception e) {
                handleException(e, this);
            }
        }
        else {
            AppUtility.createAndDisplayToast(this,
                    getString(R.string.incorrect_username_or_password),
                    Toast.LENGTH_LONG);
        }
    }

    private void exitApplication() {
        createExitAlertDialogWithConsentAndExit(this,
                getString(exit_dialog_title),
                getString(exit_dialog_message),
                getString(alert_dialog_yes),
                getString(alert_dialog_no));
    }
}