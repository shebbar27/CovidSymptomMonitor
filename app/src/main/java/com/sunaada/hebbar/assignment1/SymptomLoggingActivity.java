package com.sunaada.hebbar.assignment1;

import static android.widget.AdapterView.*;
import static com.sunaada.hebbar.assignment1.AppUtility.*;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.HashMap;

public class SymptomLoggingActivity extends AppCompatActivity
        implements OnClickListener, OnItemSelectedListener, OnRatingBarChangeListener {

    private static final float defaultRating = 0.0f;
    private final HashMap<String, Float> symptomsRating = new HashMap<>();
    private Spinner symptomsSpinner;
    private RatingBar symptomsRatingBar;
    private String latestRecordID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_logging);
        this.initializeSymptomsLogging();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            this.latestRecordID = bundle.getString(SymptomsDbHelper.RECORD_ID_KEY);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.updateSymptomsRatingBar();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // NOOP
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        this.updateRatingForSymptom(rating);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.upload_symptoms_button) {
            createAndDisplayToast(this,
                    getString(R.string.uploading_symptoms_data),
                    Toast.LENGTH_LONG);
            this.performDataBaseUpdate();
            createAndDisplayToast(this, getString(R.string.uploading_symptoms_data_success));
        }
    }

    private void initializeSymptomsLogging() {
        this.initializeSymptomsSpinner();
        this.initializeSymptomsRatingBar();
        this.initializeUploadSymptomsButton();
    }

    private void initializeSymptomsSpinner() {
        Resources res = getResources();
        String[] symptoms = res.getStringArray(R.array.select_symptom_spinner_items);
        this.symptomsSpinner = findViewById(R.id.select_symptom_spinner);
        this.symptomsSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> symptomsSpinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                symptoms
        );

        symptomsSpinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        this.symptomsSpinner.setAdapter(symptomsSpinnerAdapter);

        for(String symptom: symptoms) {
            this.symptomsRating.put(symptom, defaultRating);
        }
    }

    private void initializeSymptomsRatingBar() {
        this.symptomsRatingBar = findViewById(R.id.symptom_rating_bar);
        this.symptomsRatingBar.setOnRatingBarChangeListener(this);
        this.updateSymptomsRatingBar();
    }

    private void initializeUploadSymptomsButton() {
        Button uploadSymptomButton = findViewById(R.id.upload_symptoms_button);
        uploadSymptomButton.setOnClickListener(this);
    }

    private void updateSymptomsRatingBar() {
        String symptom = this.symptomsSpinner.getSelectedItem().toString();
        if(this.symptomsRating.containsKey(symptom)) {
            try {
                //noinspection ConstantConditions
                this.symptomsRatingBar.setRating(this.symptomsRating.get(symptom));
            }
            catch(NullPointerException e) {
                handleException(e, this);
            }
        }
    }

    private void updateRatingForSymptom(float rating) {
        String symptom = this.symptomsSpinner.getSelectedItem().toString();
        if(this.symptomsRating.containsKey(symptom))
            this.symptomsRating.put(symptom, rating);
    }

    private void performDataBaseUpdate() {
        SymptomsDbHelper symptomsDbHelper = new SymptomsDbHelper(getApplicationContext());
        SQLiteDatabase db = symptomsDbHelper.getWritableDatabase();
        Float[] symptomRatings = this.symptomsRating.values().toArray(new Float[0]);
        final String whereClause = SymptomsDbHelper.RECORD_ID + "=?";
        final String[] whereArgs = { this.latestRecordID };
        db.update(SymptomsDbHelper.SYMPTOMS_TABLE_NAME,
                symptomsDbHelper.getDatabaseRowForInserting(symptomRatings),
                whereClause,
                whereArgs);
        db.close();
    }
}