package com.indooratlas.android.sdk.examples.foregroundservice;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;

@SdkExample(description = R.string.example_foregroundservice_description)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "ForeGroundServiceExample/MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foreground);

        Button startButton = findViewById(R.id.button1);
        Button stopButton = findViewById(R.id.button2);

        // Start foreground service will create persistent notification with the "Start" and "Stop"
        // buttons for positioning
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // Check if notifications are enabled for my app.
        // Needed to show IA positioning updates in the notification aka Action bar.
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean areNotificationsEnabled = notificationManager.areNotificationsEnabled();

        Log.d(TAG, "onResume, areNotificationsEnabled : "+areNotificationsEnabled);

        if (!areNotificationsEnabled) {
            // Notifications are disabled for your app. You might want to notify the user or take appropriate action.
            Intent intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            startActivity(intent);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                Log.d(TAG, "Starting Foreground Service with Intent");
                // After starting the foreground service, you can close the example app and continue
                // using the foreground service notification
                Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
                startIntent.setAction(ForegroundService.STARTFOREGROUND_ACTION);
                startService(startIntent);
                break;
            case R.id.button2:
                Log.d(TAG, "Stopping Foreground Service with Intent");
                // To close the foreground service, "Stop foreground service" button must be pressed
                Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
                stopIntent.setAction(ForegroundService.STOPFOREGROUND_ACTION);
                startService(stopIntent);
                break;

            default:
                break;
        }

    }
}
