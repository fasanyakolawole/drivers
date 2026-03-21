package com.naijameals.driver.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.naijameals.driver.MainActivity;
import com.naijameals.driver.NewOrderActivity;
import com.naijameals.driver.R;
import com.naijameals.driver.api.ApiClient;
import com.naijameals.driver.api.DriverApi;
import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.data.AuthRepository;
import com.naijameals.driver.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Foreground service that sends location to backend every 30 seconds when driver is online.
 * Keeps running when app is minimized or closed (until user goes offline or force-stops).
 */
public class LocationForegroundService extends Service {
    private static final String CHANNEL_ID = "location_updates_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private AuthRepository authRepo;
    private Handler handler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        authRepo = new AuthRepository(this);
        // Ensure API client has token (in case process was restarted)
        String token = authRepo.getToken();
        if (token != null) ApiClient.getInstance().setToken(token);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        startLocationUpdateLoop();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopLocationUpdateLoop();
        super.onDestroy();
    }

    private Notification createNotification() {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Naija Meals Driver")
                .setContentText("Sharing your location while online")
                .setSmallIcon(R.drawable.ic_location_on)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Updates",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows when driver is online and sharing location");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void startLocationUpdateLoop() {
        stopLocationUpdateLoop();
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUserOnline()) {
                    stopSelf();
                    return;
                }
                sendLocationToBackend();
                handler.postDelayed(this, Constants.LOCATION_UPDATE_INTERVAL_MS);
            }
        };
        // Send first update immediately, then every 30 seconds
        sendLocationToBackend();
        handler.postDelayed(updateRunnable, Constants.LOCATION_UPDATE_INTERVAL_MS);
    }

    private void stopLocationUpdateLoop() {
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        handler = null;
        updateRunnable = null;
    }

    private boolean isUserOnline() {
        com.naijameals.driver.api.models.User user = authRepo.getUser();
        return user != null && user.isOnline;
    }

    private void sendLocationToBackend() {
        if (isUpdating || !isUserOnline()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isUpdating = true;
        CancellationTokenSource cts = new CancellationTokenSource();
        Task<Location> task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.getToken()
        );

        task.addOnSuccessListener(location -> {
            if (location != null && isUserOnline()) {
                Map<String, Object> body = new HashMap<>();
                body.put("latitude", location.getLatitude());
                body.put("longitude", location.getLongitude());
                body.put("isOnline", true);

                DriverApi api = ApiClient.getInstance().getApi();
                api.updateLocation(body).enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        isUpdating = false;
                        if (response.isSuccessful() && response.body() != null && response.body().user != null
                                && Boolean.TRUE.equals(response.body().user.newOrder)) {
                            showNewOrderAlert();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        isUpdating = false;
                    }
                });
            } else {
                isUpdating = false;
            }
        });
        task.addOnFailureListener(e -> isUpdating = false);
    }

    private void showNewOrderAlert() {
        Intent intent = new Intent(this, NewOrderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }
}
