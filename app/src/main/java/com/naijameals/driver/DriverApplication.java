package com.naijameals.driver;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Application class for driver app. Initializes Firebase and requests FCM token.
 */
public class DriverApplication extends Application {
    private static final String TAG = "DriverApp";
    public static final String NOTIFICATION_CHANNEL_ID = "driver_alerts";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        requestFcmToken();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "New Order Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new orders - uses long alarm and continuous vibration");
            channel.enableVibration(true);
            // Continuous vibration: vibrate 3 sec, brief pause 200ms, repeat (for ~1 min until user responds)
            channel.setVibrationPattern(new long[]{0, 3000, 200, 3000, 200, 3000, 200, 3000, 200, 3000, 200, 3000, 200, 3000, 200, 3000, 200, 3000, 200, 3000});
            android.net.Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            channel.setSound(alarmUri, null);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void requestFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "FCM token failed: " + task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM token: " + token);
                    // Copy this token to Firebase Console > Cloud Messaging > Send test message
                    // to send notifications to this device from the dashboard.
                });
    }
}
