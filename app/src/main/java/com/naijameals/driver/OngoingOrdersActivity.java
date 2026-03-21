package com.naijameals.driver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.naijameals.driver.api.models.Delivery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ongoing Orders - same as React OngoingOrdersScreen
 * Uses: GET /driver/deliveries, PUT status, PUT complete
 */
public class OngoingOrdersActivity extends AppCompatActivity {
    private List<Delivery> deliveries = new ArrayList<>();
    private Integer processingOrderId = null;
    private final Set<Call<?>> inFlightCalls = new HashSet<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_orders);

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new OngoingAdapter(deliveries, this::openMaps, this::makePhoneCall, this::handlePickedUp, this::handleComplete));

        swipeRefresh.setOnRefreshListener(this::fetchOrders);
        fetchOrders();
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
                            if ("assigned".equals(d.status) || "picking_up".equals(d.status) || "in_progress".equals(d.status)) {
                                deliveries.add(d);
                            }
                        }
                        recyclerView.getAdapter().notifyDataSetChanged();
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
                Toast.makeText(OngoingOrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openMaps(String address) {
        if (address == null || address.isEmpty()) return;
        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address));
        startActivity(new Intent(Intent.ACTION_VIEW, gmmIntentUri));
    }

    private void makePhoneCall(String number) {
        if (number == null || number.isEmpty()) return;
        String cleaned = number.replaceAll("[\\s\\-\\(\\)]", "");
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleaned)));
    }

    private void handlePickedUp(int deliveryId) {
        if (processingOrderId != null && processingOrderId == deliveryId) return;

        new AlertDialog.Builder(this)
                .setTitle("Mark as Picked Up")
                .setMessage("Are you sure you want to mark this order as picked up?")
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Yes, Mark as Picked Up", (d, w) -> {
                    processingOrderId = deliveryId;
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "in_progress");

                    DriverApi api = ApiClient.getInstance().getApi();
                    Call<ApiResponse<Object>> call = api.updateDeliveryStatus(deliveryId, body);
                    inFlightCalls.add(call);
                    call.enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> response) {
                            inFlightCalls.remove(c);
                            if (isDestroyed()) return;
                            processingOrderId = null;
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(OngoingOrdersActivity.this, "Order marked as picked up!", Toast.LENGTH_SHORT).show();
                                fetchOrders();
                            } else {
                                Toast.makeText(OngoingOrdersActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                            inFlightCalls.remove(c);
                            if (isDestroyed()) return;
                            processingOrderId = null;
                            Toast.makeText(OngoingOrdersActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void handleComplete(int deliveryId) {
        if (processingOrderId != null && processingOrderId == deliveryId) return;

        new AlertDialog.Builder(this)
                .setTitle("Complete Delivery")
                .setMessage("Are you sure you have completed this delivery?")
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Complete", (d, w) -> {
                    processingOrderId = deliveryId;
                    DriverApi api = ApiClient.getInstance().getApi();
                    Call<ApiResponse<Object>> call = api.completeDelivery(deliveryId, new HashMap<>());
                    inFlightCalls.add(call);
                    call.enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> response) {
                            inFlightCalls.remove(c);
                            if (isDestroyed()) return;
                            processingOrderId = null;
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(OngoingOrdersActivity.this, "Delivery completed!", Toast.LENGTH_SHORT).show();
                                fetchOrders();
                                startActivity(new Intent(OngoingOrdersActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(OngoingOrdersActivity.this, "Failed to complete", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                            inFlightCalls.remove(c);
                            if (isDestroyed()) return;
                            processingOrderId = null;
                            Toast.makeText(OngoingOrdersActivity.this, "Failed to complete", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }
}
