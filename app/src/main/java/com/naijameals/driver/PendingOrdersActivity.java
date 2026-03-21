package com.naijameals.driver;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.naijameals.driver.api.ApiClient;
import com.naijameals.driver.api.DriverApi;
import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.api.models.CalculateEtaRequest;
import com.naijameals.driver.api.models.Delivery;
import com.naijameals.driver.data.AuthRepository;
import com.naijameals.driver.service.LocationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pending Orders - same as React PendingOrdersScreen
 * Uses: GET /driver/deliveries, POST accept, POST decline, POST calculate-eta
 */
public class PendingOrdersActivity extends AppCompatActivity {
    private AuthRepository authRepo;
    private LocationService locationService;
    private List<Delivery> deliveries = new ArrayList<>();
    private Location deviceLocation;
    private Map<Integer, DistanceInfo> distances = new HashMap<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View tvEmpty;
    private final Set<Call<?>> inFlightCalls = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_orders);

        authRepo = new AuthRepository(this);
        locationService = new LocationService(this);

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PendingAdapter(deliveries, distances,
                this::handleAccept, this::handleDecline));

        swipeRefresh.setOnRefreshListener(this::fetchOrders);
        fetchOrders();
        getDeviceLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchOrders();
    }

    @Override
    protected void onDestroy() {
        for (Call<?> call : inFlightCalls) {
            if (call != null && !call.isCanceled()) call.cancel();
        }
        inFlightCalls.clear();
        super.onDestroy();
    }

    private void fetchOrders() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        DriverApi api = ApiClient.getInstance().getApi();
        Call<ApiResponse<Object>> call = api.getDeliveries();
        inFlightCalls.add(call);
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> response) {
                inFlightCalls.remove(c);
                if (isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<Delivery> list = response.body().deliveries;
                    if (list != null) {
                        deliveries.clear();
                        for (Delivery d : list) {
                            if ("pending".equals(d.status)) deliveries.add(d);
                        }
                        ((PendingAdapter) recyclerView.getAdapter()).notifyDataSetChanged();
                        if (deviceLocation != null) calculateDistances();
                    }
                    tvEmpty.setVisibility(deliveries.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                inFlightCalls.remove(c);
                if (isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PendingOrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDeviceLocation() {
        locationService.getCurrentLocation(this, new LocationService.LocationCallback() {
            @Override
            public void onLocation(Location location) {
                if (location != null) {
                    deviceLocation = location;
                    calculateDistances();
                } else {
                    com.naijameals.driver.api.models.User u = authRepo.getUser();
                    if (u != null && u.latitude != null && u.longitude != null) {
                        deviceLocation = new Location("");
                        deviceLocation.setLatitude(u.latitude);
                        deviceLocation.setLongitude(u.longitude);
                        calculateDistances();
                    }
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void calculateDistances() {
        if (deviceLocation == null || deliveries.isEmpty()) return;

        for (Delivery d : deliveries) {
            DistanceInfo info = new DistanceInfo();
            distances.put(d.id, info);

            // Pickup: driver → restaurant
            if (d.restaurant != null && (d.restaurant.latitude != null || d.restaurant.address != null)) {
                info.pickupLoading = true;
                String pickupFallback = null;
                if (d.restaurant.latitude != null && d.restaurant.longitude != null) {
                    double km = haversineKm(deviceLocation.getLatitude(), deviceLocation.getLongitude(),
                            d.restaurant.latitude, d.restaurant.longitude);
                    pickupFallback = formatDist(km) + " • ~" + formatDur(km);
                }
                calculateEta(d.id, "pickup", deviceLocation.getLatitude(), deviceLocation.getLongitude(),
                        d.restaurant.latitude, d.restaurant.longitude, buildFullAddress(d.restaurant.address, d.restaurant.postcode), pickupFallback);
            } else {
                info.pickupLoading = false;
                updateDistance(d.id, "pickup", false, "—", "—");
            }

            // Dropoff: restaurant → customer
            String deliveryAddress = buildFullAddress(d.address, d.postcode);
            boolean hasDeliveryDest = d.latitude != null || (deliveryAddress != null && !deliveryAddress.trim().isEmpty());
            if (d.restaurant != null && hasDeliveryDest) {
                info.dropoffLoading = true;
                String dropoffFallback = null;
                if (d.restaurant.latitude != null && d.restaurant.longitude != null
                        && d.latitude != null && d.longitude != null) {
                    double km = haversineKm(d.restaurant.latitude, d.restaurant.longitude, d.latitude, d.longitude);
                    dropoffFallback = formatDist(km) + " • ~" + formatDur(km);
                }
                String restaurantAddress = buildFullAddress(d.restaurant.address, d.restaurant.postcode);
                if (d.restaurant.latitude != null && d.restaurant.longitude != null) {
                    calculateEta(d.id, "dropoff", d.restaurant.latitude, d.restaurant.longitude,
                            d.latitude, d.longitude, deliveryAddress, dropoffFallback);
                } else if (restaurantAddress != null && !restaurantAddress.trim().isEmpty()) {
                    calculateEtaByAddress(d.id, "dropoff", restaurantAddress,
                            d.latitude, d.longitude, deliveryAddress, dropoffFallback);
                } else {
                    info.dropoffLoading = false;
                    updateDistance(d.id, "dropoff", false, "—", "—");
                }
            } else {
                info.dropoffLoading = false;
                updateDistance(d.id, "dropoff", false, "—", "—");
            }
        }
        runOnUiThread(() -> {
            if (!isDestroyed() && recyclerView.getAdapter() != null) {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String formatDist(double km) {
        if (km < 0.001) return "0 m";
        if (km < 1) return String.format(Locale.UK, "%.0f m", km * 1000);
        return String.format(Locale.UK, "%.1f mi", km * 0.621371);
    }

    private String formatDur(double km) {
        int mins = (int) Math.max(1, (km / 30.0) * 60);
        return mins < 60 ? mins + " mins" : (mins / 60) + " hr";
    }

    private String buildFullAddress(String address, String postcode) {
        if (address == null || address.trim().isEmpty()) return postcode != null ? postcode.trim() : null;
        if (postcode == null || postcode.trim().isEmpty()) return address.trim();
        return (address.trim() + ", " + postcode.trim()).trim();
    }

    private void calculateEtaByAddress(int deliveryId, String type, String originAddress,
                                        Double destLat, Double destLng, String destAddress, String fallback) {
        CalculateEtaRequest body;
        if (destLat != null && destLng != null) {
            body = CalculateEtaRequest.fromAddressAndCoords(originAddress, destLat, destLng);
        } else if (destAddress != null && !destAddress.trim().isEmpty()) {
            body = CalculateEtaRequest.fromAddresses(originAddress, destAddress);
        } else {
            updateDistance(deliveryId, type, false, "—", "—");
            return;
        }
        callCalculateEta(deliveryId, type, body, fallback);
    }

    private void calculateEta(int deliveryId, String type, double origLat, double origLng,
                             Double destLat, Double destLng, String destAddress, String fallback) {
        CalculateEtaRequest body;
        if (destLat != null && destLng != null) {
            body = CalculateEtaRequest.fromCoords(origLat, origLng, destLat, destLng);
        } else if (destAddress != null && !destAddress.trim().isEmpty()) {
            body = CalculateEtaRequest.fromCoordsAndAddress(origLat, origLng, destAddress);
        } else {
            updateDistance(deliveryId, type, false, "—", "—");
            return;
        }
        callCalculateEta(deliveryId, type, body, fallback);
    }

    private void callCalculateEta(final int deliveryId, final String type, CalculateEtaRequest body, final String fallback) {
        DriverApi api = ApiClient.getInstance().getApi();
        Call<ApiResponse<Object>> call = api.calculateEta(body);
        inFlightCalls.add(call);
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> response) {
                inFlightCalls.remove(c);
                if (isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    String dist = response.body().distance != null ? response.body().distance : "";
                    String dur = response.body().duration != null ? response.body().duration : "";
                    if (!dist.isEmpty() || !dur.isEmpty()) {
                        updateDistance(deliveryId, type, false, dist, dur);
                    } else if (fallback != null) {
                        String[] parts = fallback.split(" • ~", 2);
                        updateDistance(deliveryId, type, false, parts[0], parts.length > 1 ? "~" + parts[1] : "");
                    } else {
                        updateDistance(deliveryId, type, false, "—", "—");
                    }
                } else if (fallback != null) {
                    String[] parts = fallback.split(" • ~", 2);
                    updateDistance(deliveryId, type, false, parts[0], parts.length > 1 ? "~" + parts[1] : "");
                } else {
                    updateDistance(deliveryId, type, false, "—", "—");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                inFlightCalls.remove(c);
                if (!isDestroyed()) {
                    if (fallback != null) {
                        String[] parts = fallback.split(" • ~", 2);
                        updateDistance(deliveryId, type, false, parts[0], parts.length > 1 ? "~" + parts[1] : "");
                    } else {
                        updateDistance(deliveryId, type, false, "—", "—");
                    }
                }
            }
        });
    }

    private void updateDistance(int deliveryId, String type, boolean loading, String distance, String duration) {
        DistanceInfo info = distances.get(deliveryId);
        if (info == null) info = new DistanceInfo();
        if ("pickup".equals(type)) {
            info.pickupDist = distance;
            info.pickupDur = duration;
            info.pickupLoading = loading;
        } else {
            info.dropoffDist = distance;
            info.dropoffDur = duration;
            info.dropoffLoading = loading;
        }
        distances.put(deliveryId, info);
        runOnUiThread(() -> {
            if (isDestroyed()) return;
            RecyclerView.Adapter a = recyclerView.getAdapter();
            if (a != null) a.notifyDataSetChanged();
        });
    }

    private void handleAccept(int deliveryId) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Delivery")
                .setMessage("Are you sure you want to accept this delivery?")
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Yes, Accept", (d, w) -> {
                    DriverApi api = ApiClient.getInstance().getApi();
                    Call<ApiResponse<Object>> call = api.acceptDelivery(deliveryId, new HashMap<>());
                    inFlightCalls.add(call);
                    call.enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> response) {
                            inFlightCalls.remove(c);
                            if (isDestroyed()) return;
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(PendingOrdersActivity.this, "Delivery accepted!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(PendingOrdersActivity.this, OngoingOrdersActivity.class));
                                finish();
                            } else {
                                Toast.makeText(PendingOrdersActivity.this, response.body() != null ? response.body().message : "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                            inFlightCalls.remove(c);
                            if (!isDestroyed()) Toast.makeText(PendingOrdersActivity.this, "Failed to accept", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void handleDecline(int deliveryId) {
        new AlertDialog.Builder(this)
                .setTitle("Decline Delivery")
                .setMessage("Are you sure you want to decline this delivery?")
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Decline", (d, w) -> {
                    DriverApi api = ApiClient.getInstance().getApi();
                    Call<ApiResponse<Object>> call = api.declineDelivery(deliveryId, new HashMap<>());
                    inFlightCalls.add(call);
                    call.enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> response) {
                            inFlightCalls.remove(c);
                            if (isDestroyed()) return;
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(PendingOrdersActivity.this, "Delivery declined.", Toast.LENGTH_SHORT).show();
                                fetchOrders();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                            inFlightCalls.remove(c);
                        }
                    });
                })
                .show();
    }

    static class DistanceInfo {
        boolean pickupLoading;
        boolean dropoffLoading;
        String pickupDist = "", pickupDur = "", dropoffDist = "", dropoffDur = "";
    }
}
