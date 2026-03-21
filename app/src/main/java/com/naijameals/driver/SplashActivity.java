package com.naijameals.driver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.naijameals.driver.data.AuthRepository;

import java.lang.ref.WeakReference;

/**
 * Shows delivery image as splash, then navigates to MainActivity or LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION_MS = 2500;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Opened from FCM notification tap? Go straight to NewOrderActivity to ring
        if (wasOpenedFromFcmNotification()) {
            startActivity(new Intent(this, NewOrderActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        AuthRepository authRepo = new AuthRepository(this);
        String token = authRepo.getToken();
        com.naijameals.driver.api.models.User user = authRepo.getUser();

        WeakReference<SplashActivity> ref = new WeakReference<>(this);
        handler.postDelayed(() -> {
            SplashActivity activity = ref.get();
            if (activity == null || activity.isDestroyed()) return;
            if (token != null && user != null) {
                activity.startActivity(new Intent(activity, MainActivity.class));
            } else {
                activity.startActivity(new Intent(activity, LoginActivity.class));
            }
            activity.finish();
        }, SPLASH_DURATION_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private boolean wasOpenedFromFcmNotification() {
        if (getIntent() == null || getIntent().getExtras() == null) return false;
        Bundle extras = getIntent().getExtras();
        // FCM adds these when notification was tapped; custom data keys also present
        return extras.containsKey("google.message_id") || extras.containsKey("gcm.notification.body")
                || "new_order".equals(extras.getString("type"));
    }
}
