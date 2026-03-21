package com.naijameals.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.naijameals.driver.api.ApiClient;
import com.naijameals.driver.api.DriverApi;
import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.api.models.Delivery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Completed Orders - same as React CompletedOrdersScreen
 * Uses: GET /driver/deliveries
 */
public class CompletedOrdersActivity extends AppCompatActivity {
    private List<Delivery> deliveries = new ArrayList<>();
    private final Set<Call<?>> inFlightCalls = new HashSet<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_orders);

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new CompletedAdapter(deliveries));

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
                            if ("completed".equals(d.status)) deliveries.add(d);
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
                Toast.makeText(CompletedOrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
