package com.sunaada.hebbar.assignment1;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SymptomsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    private static final String SYMPTOM_MONITOR_DATA_BASE_NAME = "symptomMonitorDB";
    public static final String SYMPTOMS_TABLE_NAME = "symptomsTable";
    public static final String RECORD_ID= "recordId";
    public static final String RECORD_ID_KEY= "record_id_key";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + SYMPTOMS_TABLE_NAME + "("
                    + " recordId INTEGER PRIMARY KEY autoincrement, "
                    + " heartRate NUMERIC, "
                    + " respiratoryRate NUMERIC, "
                    + " symptomsNausea NUMERIC, "
                    + " symptomsHeadache NUMERIC, "
                    + " symptomsDiarrhea NUMERIC, "
                    + " symptomsSoarThroat NUMERIC, "
                    + " symptomsFever NUMERIC, "
                    + " symptomsMuscleAche NUMERIC, "
                    + " symptomsLossOfSmellOrTaste NUMERIC, "
                    + " symptomsCough NUMERIC, "
                    + " symptomsShortnessOfBreath NUMERIC, "
                    + " symptomsFeelingTired NUMERIC ); ";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SYMPTOMS_TABLE_NAME;

    public SymptomsDbHelper(Context context) {
        super(context, SYMPTOM_MONITOR_DATA_BASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(SQL_CREATE_TABLE);
        }catch (SQLiteException e) {
            e.printStackTrace();
            Log.d("db transaction failed", "SQLite database transaction failed: " + e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        this.onCreate(db);
    }

    public ContentValues getDatabaseRowForInserting(float heartRate, float respiratoryRate) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("heartRate", heartRate);
        contentValues.put("respiratoryRate", respiratoryRate);
        return contentValues;
    }

    public ContentValues getDatabaseRowForInserting(Float[] symptomsRating) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("symptomsNausea", symptomsRating[0]);
        contentValues.put("symptomsHeadache", symptomsRating[1]);
        contentValues.put("symptomsDiarrhea", symptomsRating[2]);
        contentValues.put("symptomsSoarThroat", symptomsRating[3]);
        contentValues.put("symptomsFever", symptomsRating[4]);
        contentValues.put("symptomsMuscleAche", symptomsRating[5]);
        contentValues.put("symptomsLossOfSmellOrTaste", symptomsRating[6]);
        contentValues.put("symptomsCough", symptomsRating[7]);
        contentValues.put("symptomsShortnessOfBreath", symptomsRating[8]);
        contentValues.put("symptomsFeelingTired", symptomsRating[9]);
        return contentValues;
    }
}