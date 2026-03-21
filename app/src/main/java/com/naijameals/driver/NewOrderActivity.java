package com.naijameals.driver;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Full-screen alert shown when driver receives a new order.
 * Vibrates, plays continuous sound, and displays message.
 */
public class NewOrderActivity extends AppCompatActivity {
    private static final long ALERT_DURATION_MS = 60_000; // 1 minute

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private Runnable stopAlertsRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_new_order);

        startVibration();
        startAlarmSound();

        // Stop sound and vibration after 1 minute if user hasn't responded
        stopAlertsRunnable = () -> {
            if (!isDestroyed()) stopAlerts();
        };
        stopHandler.postDelayed(stopAlertsRunnable, ALERT_DURATION_MS);

        findViewById(R.id.btnViewOrders).setOnClickListener(v -> {
            cancelStopTimer();
            stopAlerts();
            startActivity(new Intent(this, PendingOrdersActivity.class));
            finish();
        });
        findViewById(R.id.btnDismiss).setOnClickListener(v -> {
            cancelStopTimer();
            stopAlerts();
            finish();
        });
    }

    private void startVibration() {
        // Continuous strong vibration: 3 sec on, 200ms off, repeats until user responds (or 1 min timeout)
        long[] pattern = {0, 3000, 200, 3000, 200, 3000, 200};
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 = repeat
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            } catch (Exception ignored) {}
        }
    }

    private void startAlarmSound() {
        try {
            // Use ALARM first - long, loud, persistent alarm sound; loops until user responds
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alarmUri != null) {
                mediaPlayer = MediaPlayer.create(this, alarmUri);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                }
            }
        } catch (Exception ignored) {}
    }

    private void cancelStopTimer() {
        if (stopAlertsRunnable != null) {
            stopHandler.removeCallbacks(stopAlertsRunnable);
        }
    }

    private void stopAlerts() {
        cancelStopTimer();
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception ignored) {}
        }
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopAlerts();
        super.onDestroy();
    }
}
