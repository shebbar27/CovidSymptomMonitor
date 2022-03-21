package com.sunaada.hebbar.assignment1;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import net.sqlcipher.database.SQLiteDatabase;
import java.io.File;

public class SymptomsDbHelper {

    public static final int DATABASE_VERSION = 1;
    private static final String SYMPTOM_MONITOR_DATA_BASE_NAME = "symptomMonitorDB";
    public static final String SYMPTOMS_TABLE_NAME = "symptomsTable";
    public static final String RECORD_ID= "recordId";
    public static final String RECORD_ID_KEY= "record_id_key";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + SYMPTOMS_TABLE_NAME + "("
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
                    + " symptomsFeelingTired NUMERIC, "
                    + " locationX TEXT, "
                    + " locationY TEXT, "
                    + " time TEXT); ";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SYMPTOMS_TABLE_NAME;

    public SQLiteDatabase database;

    public SymptomsDbHelper(Context context) {
        SQLiteDatabase.loadLibs(context);
        File databaseFile = context.getDatabasePath(SYMPTOM_MONITOR_DATA_BASE_NAME);
        this.database = SQLiteDatabase.openOrCreateDatabase(databaseFile, context.getString(R.string.password), null);
        try {
            this.database.execSQL(SQL_CREATE_TABLE);
            Log.d("db transaction success", "SQLite database transaction successful!");
        }
        catch (SQLiteException e) {
            e.printStackTrace();
            Log.d("db transaction failed", "SQLite database transaction failed: " + e);
        }
    }

    public static ContentValues getDatabaseRowForInserting(float heartRate, float respiratoryRate, String time) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("heartRate", heartRate);
        contentValues.put("respiratoryRate", respiratoryRate);
        contentValues.put("symptomsNausea", 0f);
        contentValues.put("symptomsHeadache", 0f);
        contentValues.put("symptomsDiarrhea", 0f);
        contentValues.put("symptomsSoarThroat", 0f);
        contentValues.put("symptomsFever", 0f);
        contentValues.put("symptomsMuscleAche", 0f);
        contentValues.put("symptomsLossOfSmellOrTaste", 0f);
        contentValues.put("symptomsCough", 0f);
        contentValues.put("symptomsShortnessOfBreath", 0f);
        contentValues.put("symptomsFeelingTired", 0f);
        contentValues.put("locationX", "0");
        contentValues.put("locationY", "0");
        contentValues.put("time", time);
        return contentValues;
    }

    public static ContentValues getDatabaseRowForUpdating(Float[] symptomsRating) {
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

    public static ContentValues getEmptyDatabaseRowForInserting() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("heartRate", 0f);
        contentValues.put("respiratoryRate", 0f);
        contentValues.put("symptomsNausea", 0f);
        contentValues.put("symptomsHeadache", 0f);
        contentValues.put("symptomsDiarrhea", 0f);
        contentValues.put("symptomsSoarThroat", 0f);
        contentValues.put("symptomsFever", 0f);
        contentValues.put("symptomsMuscleAche", 0f);
        contentValues.put("symptomsLossOfSmellOrTaste", 0f);
        contentValues.put("symptomsCough", 0f);
        contentValues.put("symptomsShortnessOfBreath", 0f);
        contentValues.put("symptomsFeelingTired", 0f);
        contentValues.put("locationX", "0");
        contentValues.put("locationY", "0");
        contentValues.put("time", "2022-01-01T00:00:00Z");
        return contentValues;
    }

    public static ContentValues getDatabaseRowForInserting(String locationX,
                                                    String locationY,
                                                    String time) {
        ContentValues contentValues = getEmptyDatabaseRowForInserting();
        contentValues.put("locationX", locationX);
        contentValues.put("locationY", locationY);
        contentValues.put("time", time);
        return contentValues;
    }
}