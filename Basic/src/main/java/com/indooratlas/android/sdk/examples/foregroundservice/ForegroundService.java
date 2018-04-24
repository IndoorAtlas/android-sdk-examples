package com.indooratlas.android.sdk.examples.foregroundservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.examples.R;

public class ForegroundService extends Service {

    public static final String MAIN_ACTION = "main_action";
    public static final String PAUSE_ACTION = "pause_positioning_action";
    public static final String START_ACTION = "start_positioning_action";
    public static final String STOP_ACTION = "stop_positioning_action";
    public static final String STARTFOREGROUND_ACTION = "startforeground";
    public static final String STOPFOREGROUND_ACTION = "stopforeground";

    public static final int NOTIFICATION_ID = 101;
    private static final String NOTIFICATION_CHANNEL_ID = "example_notification_channel";

    private static final String LOG_TAG = "IAForegroundExample";

    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel =
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications",
                            NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("IndoorAtlas Foreground Service Example")
                .setTicker("IndoorAtlas Foreground Service Example")
                .setSmallIcon(R.drawable.ic_launcher);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IALocation location = IALocation.from(intent);
        if (location != null) {
            Log.i(LOG_TAG, "Got IA Location: " + location);
            mBuilder.setContentText(location.getLatitude() + ", " + location.getLongitude());
            NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            return START_STICKY;
        }


        if (intent.getAction().equals(ForegroundService.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent ");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(ForegroundService.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Intent previousIntent = new Intent(this, ForegroundService.class);
            previousIntent.setAction(ForegroundService.PAUSE_ACTION);
            PendingIntent ppauseIntent = PendingIntent.getService(this, 0,
                    previousIntent, 0);

            Intent playIntent = new Intent(this, ForegroundService.class);
            playIntent.setAction(ForegroundService.START_ACTION);
            PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                    playIntent, 0);

            Intent nextIntent = new Intent(this, ForegroundService.class);
            nextIntent.setAction(ForegroundService.STOP_ACTION);
            PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                    nextIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_launcher);

            Notification notification = mBuilder
                    .setLargeIcon(icon)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_pause,
                            "Pause", ppauseIntent)
                    .addAction(android.R.drawable.ic_media_play, "Start",
                            pplayIntent)
                    .addAction(android.R.drawable.ic_media_ff, "Stop",
                            pstopIntent).build();


            startForeground(NOTIFICATION_ID, notification);

        } else if (intent.getAction().equals(ForegroundService.PAUSE_ACTION)) {
            IALocationManager manager = IALocationManager.create(this);
            PendingIntent stopIntent = PendingIntent.getService(this, 0,
                    new Intent(this, ForegroundService.class), 0);
            manager.removeLocationUpdates(stopIntent);
            manager.destroy();

            Log.i(LOG_TAG, "Clicked Pause ");
        } else if (intent.getAction().equals(ForegroundService.START_ACTION)) {
            IALocationManager manager = IALocationManager.create(this);
            PendingIntent requestIntent = PendingIntent.getService(this, 0,
                    new Intent(this, ForegroundService.class), 0);
            manager.requestLocationUpdates(IALocationRequest.create(), requestIntent);
            manager.destroy();

            Log.i(LOG_TAG, "Clicked Start");
        } else if (intent.getAction().equals(ForegroundService.STOP_ACTION)) {
            stopForeground(true);
            stopSelf();

            Log.i(LOG_TAG, "Clicked Stop");
        } else if (intent.getAction().equals(ForegroundService.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
		return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case if services are bound (Bound Services).
        return null;
    }
}
