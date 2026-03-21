package com.naijameals.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.naijameals.driver.api.ApiClient;
import com.naijameals.driver.api.DriverApi;
import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.api.models.Delivery;
import com.naijameals.driver.api.models.User;
import com.google.firebase.messaging.FirebaseMessaging;
import com.naijameals.driver.data.AuthRepository;
import com.naijameals.driver.service.LocationForegroundService;
import com.naijameals.driver.service.LocationService;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Driver Home - same as React DriverHomeScreen
 */
public class MainActivity extends AppCompatActivity {
    private AuthRepository authRepo;
    private LocationService locationService;
    private User user;
    private boolean togglingStatus = false;

    private TextView tvDriverName, tvStatus, tvWeeklyEarnings, tvPendingCount, tvOngoingCount, tvCompletedCount;
    private android.view.View tvLogout;
    private Button btnToggleOnline;
    private android.view.View viewOnlineDot;

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private final Set<Call<?>> inFlightCalls = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authRepo = new AuthRepository(this);
        locationService = new LocationService(this);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (isDestroyed()) return;
                    Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    if (Boolean.TRUE.equals(fine)) {
                        performToggleOnline();
                    } else {
                        Toast.makeText(this, "Location permission required to go online", Toast.LENGTH_LONG).show();
                        togglingStatus = false;
                        btnToggleOnline.setEnabled(true);
                    }
                }
        );
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isDestroyed()) performToggleOnline();
                }
        );

        authRepo.initializeAuth((token, storedUser) -> {
            if (isDestroyed()) return;
            if (token == null || storedUser == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            user = storedUser;
            setupUI();
        });
    }

    private void setupUI() {
        setContentView(R.layout.activity_driver_home);

        tvDriverName = findViewById(R.id.tvDriverName);
        tvStatus = findViewById(R.id.tvStatus);
        tvWeeklyEarnings = findViewById(R.id.tvWeeklyEarnings);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvLogout = findViewById(R.id.tvLogout);
        btnToggleOnline = findViewById(R.id.btnToggleOnline);
        viewOnlineDot = findViewById(R.id.viewOnlineDot);

        updateUserUI();
        fetchOrderCounts();

        tvLogout.setOnClickListener(v -> handleLogout());
        btnToggleOnline.setOnClickListener(v -> handleToggleOnlineStatus());
        findViewById(R.id.cardNewOrders).setOnClickListener(v -> startActivity(new Intent(this, PendingOrdersActivity.class)));
        findViewById(R.id.cardOngoing).setOnClickListener(v -> startActivity(new Intent(this, OngoingOrdersActivity.class)));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> startActivity(new Intent(this, CompletedOrdersActivity.class)));

        startOrStopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) {
            user = authRepo.getUser();
            updateUserUI();
            fetchOrderCounts();
        }
    }

    @Override
    protected void onDestroy() {
        for (Call<?> call : inFlightCalls) {
            if (call != null && !call.isCanceled()) call.cancel();
        }
        inFlightCalls.clear();
        super.onDestroy();
    }

    private void updateUserUI() {
        if (user == null) return;
        tvDriverName.setText(user.name != null ? user.name : "Driver");
        tvStatus.setText(user.isOnline ? "Online" : "Offline");
        viewOnlineDot.setBackgroundResource(user.isOnline ? R.drawable.online_dot_active : R.drawable.online_dot);
        btnToggleOnline.setText(user.isOnline ? "Go Offline" : "Go Online");
    }

    private void fetchOrderCounts() {
        DriverApi api = ApiClient.getInstance().getApi();
        Call<ApiResponse<Object>> call = api.getDeliveries();
        inFlightCalls.add(call);
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                inFlightCalls.remove(call);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    ApiResponse<Object> body = response.body();
                    List<Delivery> deliveries = parseDeliveries(body);
                    int pending = 0, ongoing = 0, completed = 0;
                    double totalEarnings = 0;
                    if (deliveries != null) {
                        for (Delivery d : deliveries) {
                            if ("pending".equals(d.status)) pending++;
                            else if ("assigned".equals(d.status) || "picking_up".equals(d.status) || "in_progress".equals(d.status)) ongoing++;
                            else if ("completed".equals(d.status)) {
                                completed++;
                                totalEarnings += parseDeliveryPrice(d.deliveryPrice);
                            }
                        }
                    }
                    final int p = pending;
                    final int o = ongoing;
                    final int c = completed;
                    final double earnings = totalEarnings;
                    runOnUiThread(() -> {
                        if (isDestroyed()) return;
                        tvPendingCount.setText(String.valueOf(p));
                        tvOngoingCount.setText(String.valueOf(o));
                        tvCompletedCount.setText(String.valueOf(c));
                        tvWeeklyEarnings.setText(String.format(Locale.UK, "£%.2f", earnings));
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                inFlightCalls.remove(call);
            }
        });
    }

    private List<Delivery> parseDeliveries(ApiResponse<Object> body) {
        if (body.deliveries != null) return body.deliveries;
        return null;
    }

    private double parseDeliveryPrice(String price) {
        if (price == null || price.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(price.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void startOrStopLocationUpdates() {
        if (user != null && user.isOnline) {
            // Start foreground service - keeps sending location every 30 sec even when app minimized/closed
            Intent serviceIntent = new Intent(this, LocationForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            stopService(new Intent(this, LocationForegroundService.class));
        }
    }

    private void handleToggleOnlineStatus() {
        if (togglingStatus) return;
        togglingStatus = true;
        btnToggleOnline.setEnabled(false);

        if (!user.isOnline) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
            performToggleOnline();
        } else {
            WeakReference<MainActivity> ref = new WeakReference<>(this);
            authRepo.toggleOnlineStatus(null, null, null, new AuthRepository.ToggleCallback() {
                @Override
                public void onSuccess(User u) {
                    runOnUiThread(() -> {
                        MainActivity a = ref.get();
                        if (a == null || a.isDestroyed()) return;
                        a.user = u;
                        a.authRepo.saveUser(u);
                        a.updateUserUI();
                        a.startOrStopLocationUpdates();
                        a.togglingStatus = false;
                        a.btnToggleOnline.setEnabled(true);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        MainActivity a = ref.get();
                        if (a == null || a.isDestroyed()) return;
                        Toast.makeText(a, message, Toast.LENGTH_SHORT).show();
                        a.togglingStatus = false;
                        a.btnToggleOnline.setEnabled(true);
                    });
                }
            });
        }
    }

    private void performToggleOnline() {
        WeakReference<MainActivity> ref = new WeakReference<>(this);
        locationService.getCurrentLocation(this, new LocationService.LocationCallback() {
            @Override
            public void onLocation(android.location.Location location) {
                MainActivity a = ref.get();
                if (a == null || a.isDestroyed()) return;
                if (location == null) {
                    runOnUiThread(() -> {
                        MainActivity act = ref.get();
                        if (act == null || act.isDestroyed()) return;
                        new AlertDialog.Builder(act)
                                .setTitle("Location Required")
                                .setMessage("Unable to get your location. Please enable location services.")
                                .setPositiveButton("OK", null)
                                .show();
                        act.togglingStatus = false;
                        act.btnToggleOnline.setEnabled(true);
                    });
                    return;
                }
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    String fcmToken = task.isSuccessful() ? task.getResult() : null;
                    authRepo.toggleOnlineStatus(location.getLatitude(), location.getLongitude(), fcmToken, new AuthRepository.ToggleCallback() {
                        @Override
                        public void onSuccess(User u) {
                            runOnUiThread(() -> {
                                MainActivity act = ref.get();
                                if (act == null || act.isDestroyed()) return;
                                act.user = u;
                                act.authRepo.saveUser(u);
                                act.updateUserUI();
                                act.startOrStopLocationUpdates();
                                act.togglingStatus = false;
                                act.btnToggleOnline.setEnabled(true);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                MainActivity act = ref.get();
                                if (act == null || act.isDestroyed()) return;
                                Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
                                act.togglingStatus = false;
                                act.btnToggleOnline.setEnabled(true);
                            });
                        }
                    });
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    MainActivity act = ref.get();
                    if (act == null || act.isDestroyed()) return;
                    new AlertDialog.Builder(act)
                            .setTitle("Location Required")
                            .setMessage(message != null ? message : "Unable to get your location.")
                            .setPositiveButton("OK", null)
                            .show();
                    act.togglingStatus = false;
                    act.btnToggleOnline.setEnabled(true);
                });
            }
        });
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Logout", (d, w) -> {
                    WeakReference<MainActivity> ref = new WeakReference<>(MainActivity.this);
                    authRepo.logout(() -> {
                        runOnUiThread(() -> {
                            MainActivity a = ref.get();
                            if (a == null || a.isDestroyed()) return;
                            a.startActivity(new Intent(a, LoginActivity.class));
                            a.finish();
                        });
                    });
                })
                .show();
    }
}
