package com.sunaada.hebbar.assignment1;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    private static final String CONNECTION_SPEED_DATA_FILENAME = "connectionSpeed.txt";
    private final ExecutorService executorService;

    public AlarmBroadcastReceiver() {
        super();
        this.executorService = Executors.newCachedThreadPool();
    }

    protected void finalize() {
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
    public void onReceive(Context context, Intent intent) {
        Log.d("Alarm Received", "Alarm Broadcast Message Received by Broadcast Receiver");
        Log.d("Alarm Received",
                "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                        + "Current Thread: " + Thread.currentThread());

        this.executorService.execute(() -> this.getLocationDataAndUploadItToDb(context, intent));
        this.computeConnectionSpeedAndLogData(context);
    }

    private void getLocationDataAndUploadItToDb(Context context, Intent intent) {
        Log.d("Location data","Initiating location manager to collect gps data");
        Log.d("Location data",
                "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                        + "Current Thread: " + Thread.currentThread());

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Location data", "Missing permissions! Hence unable to access location");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsProviderEnabled && intent != null) {
            try {
                Looper.prepare();
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                        location -> {
                            Log.d("Location data", "Location data: x = " + location.getLatitude() + "y = " + location.getLongitude());
                            Log.d("Location data",
                                    "In" + Thread.currentThread().getStackTrace()[1].getMethodName()
                                            + "Current Thread: " + Thread.currentThread());

                            SymptomsDbHelper symptomsDbHelper = new SymptomsDbHelper(context);
                            SQLiteDatabase db = symptomsDbHelper.getWritableDatabase();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                            String timeStamp = sdf.format(new Date());
                            db.insert(SymptomsDbHelper.SYMPTOMS_TABLE_NAME,
                                    null,
                                    symptomsDbHelper.getDatabaseRowForInserting(
                                            Double.toString(location.getLatitude()),
                                            Double.toString(location.getLongitude()),
                                            timeStamp));
                            db.close();
                        }, Looper.myLooper());
                Looper.loop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void computeConnectionSpeedAndLogData(Context context) {
        Log.d("Network speed", "Running Network speed measurement");
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("https://www.google.com", new AsyncHttpResponseHandler() {
            private long startTime;

            @Override
            public void onStart() {
                // called before request is started
                startTime = System.currentTimeMillis();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                long connectionSpeed = System.currentTimeMillis() - startTime;
                Log.d("Network speed", "Measured Network speed = " + connectionSpeed);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                String timeStamp = sdf.format(new Date());
                String connectionSpeedData = String.format(Locale.US,
                        "TimeStamp:%s, ConnectionSpeed in ms: %d",
                        timeStamp,
                        connectionSpeed);
                try {
                    FileOutputStream locationDataFile = context.openFileOutput(
                            CONNECTION_SPEED_DATA_FILENAME,
                            Context.MODE_APPEND);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(locationDataFile);
                    outputStreamWriter.write(connectionSpeedData);
                    outputStreamWriter.write(System.lineSeparator());
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    locationDataFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                e.printStackTrace();
                Log.d("Network speed", "Network speed measurement failed due to " + e);
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
                Log.d("Network speed", "Retrying network speed measurement for " + retryNo);
            }
        });
    }
}
