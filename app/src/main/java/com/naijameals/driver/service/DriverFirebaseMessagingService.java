package com.naijameals.driver.service;

import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.naijameals.driver.NewOrderActivity;

/**
 * Handles incoming FCM messages. Launches NewOrderActivity directly so it rings like a phone call.
 * IMPORTANT: For phone-call behavior when app is in background, your backend must send DATA-ONLY
 * messages (no "notification" payload). Firebase Console sends notification messages by default,
 * which the system displays - we never get control. Use your API/backend to send data-only FCM.
 */
public class DriverFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "DriverFCM";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Wake device so the ringing activity is visible (screen turns on via activity flags)
        PowerManager.WakeLock wakeLock = null;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "DriverApp:NewOrder"
            );
            wl.acquire(10_000); // 10 seconds max - activity handles screen on
            wakeLock = wl;
        }

        Intent intent = new Intent(this, NewOrderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);
        // TODO: Send token to your backend to target this device
    }
}
