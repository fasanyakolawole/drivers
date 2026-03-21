package com.naijameals.driver.service;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.naijameals.driver.api.ApiClient;
import com.naijameals.driver.api.DriverApi;
import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.utils.Constants;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Location Service - same logic as React LocationService
 * - getCurrentLocation
 * - updateLocationToBackend every 30 seconds when online
 */
public class LocationService {
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;
    private LocationCallback currentCallback;

    public interface LocationCallback {
        void onLocation(Location location);
        void onError(String message);
    }

    public interface IsOnlineChecker {
        boolean isOnline();
    }

    public LocationService(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    /**
     * Get current location with lifecycle awareness. Uses WeakReference to avoid leaking Activity
     * when the location task completes after the Activity is destroyed.
     */
    public void getCurrentLocation(Activity activity, LocationCallback callback) {
        if (activity == null || callback == null) return;
        LocationCallback wrappedCallback = wrapCallbackIfNeeded(activity, callback);
        getCurrentLocationInternal(wrappedCallback);
    }

    private LocationCallback wrapCallbackIfNeeded(Activity activity, LocationCallback callback) {
        WeakReference<Activity> activityRef = new WeakReference<>(activity);
        WeakReference<LocationCallback> callbackRef = new WeakReference<>(callback);
        return new LocationCallback() {
            @Override
            public void onLocation(Location location) {
                Activity a = activityRef.get();
                LocationCallback c = callbackRef.get();
                if (a != null && !a.isDestroyed() && c != null) {
                    c.onLocation(location);
                }
            }

            @Override
            public void onError(String message) {
                Activity a = activityRef.get();
                LocationCallback c = callbackRef.get();
                if (a != null && !a.isDestroyed() && c != null) {
                    c.onError(message);
                }
            }
        };
    }

    private void getCurrentLocationInternal(LocationCallback callback) {
        this.currentCallback = callback;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Location permission not granted");
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        Task<Location> task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        );

        task.addOnSuccessListener(location -> {
            if (location != null && currentCallback != null) {
                currentCallback.onLocation(location);
            } else {
                if (currentCallback != null) currentCallback.onError("Unable to get location");
            }
            currentCallback = null;
        });
        task.addOnFailureListener(e -> {
            if (currentCallback != null) currentCallback.onError(e.getMessage());
            currentCallback = null;
        });
    }

    public void startLocationUpdates(IsOnlineChecker isOnlineChecker) {
        stopLocationUpdates();
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isOnlineChecker.isOnline()) return;
                updateLocationToBackend(isOnlineChecker);
                updateHandler.postDelayed(this, Constants.LOCATION_UPDATE_INTERVAL_MS);
            }
        };
        if (isOnlineChecker.isOnline()) {
            updateLocationToBackend(isOnlineChecker);
        }
        updateHandler.postDelayed(updateRunnable, Constants.LOCATION_UPDATE_INTERVAL_MS);
    }

    public void stopLocationUpdates() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        updateHandler = null;
        updateRunnable = null;
    }

    private void updateLocationToBackend(IsOnlineChecker isOnlineChecker) {
        if (isUpdating || !isOnlineChecker.isOnline()) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isUpdating = true;
        getCurrentLocationInternal(new LocationCallback() {
            @Override
            public void onLocation(Location location) {
                if (!isOnlineChecker.isOnline()) {
                    isUpdating = false;
                    return;
                }
                Map<String, Object> body = new HashMap<>();
                body.put("latitude", location.getLatitude());
                body.put("longitude", location.getLongitude());
                body.put("isOnline", true);

                DriverApi api = ApiClient.getInstance().getApi();
                api.updateLocation(body).enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        isUpdating = false;
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        isUpdating = false;
                    }
                });
            }

            @Override
            public void onError(String message) {
                isUpdating = false;
            }
        });
    }
}
