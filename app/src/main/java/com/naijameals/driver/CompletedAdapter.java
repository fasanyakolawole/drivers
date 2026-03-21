package com.naijameals.driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naijameals.driver.api.models.Delivery;

import java.util.List;
import java.util.Locale;

public class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.VH> {
    private final List<Delivery> items;

    CompletedAdapter(List<Delivery> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_completed_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Delivery d = items.get(pos);
        double price = 0;
        try { price = d.deliveryPrice != null ? Double.parseDouble(d.deliveryPrice) : 0; } catch (Exception e) {}

        h.tvOrderNumber.setText("Order #" + (d.orderNumber != null ? d.orderNumber : d.id));
        h.tvPrice.setText(String.format(Locale.UK, "£%.2f", price));

        if (d.restaurant != null) {
            h.tvRestaurant.setText(d.restaurant.name);
            h.tvRestaurantAddress.setText(d.restaurant.address + ", " + d.restaurant.postcode);
        }

        h.tvCustomerName.setText(d.customerName != null ? d.customerName : "");
        h.tvCustomerName.setVisibility(d.customerName != null ? View.VISIBLE : View.GONE);
        h.tvDeliveryAddress.setText(d.address + ", " + d.postcode);
        h.tvDeliveryAddress.setOnClickListener(null);

        if (d.updatedAt != null) {
            try {
                java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(d.updatedAt.substring(0, Math.min(19, d.updatedAt.length())));
                h.tvCompletedDate.setText(date != null ? new java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.US).format(date) : d.updatedAt);
            } catch (Exception e) {
                h.tvCompletedDate.setText(d.updatedAt);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderNumber, tvPrice, tvRestaurant, tvRestaurantAddress, tvCustomerName, tvDeliveryAddress, tvCompletedDate;

        VH(View v) {
            super(v);
            tvOrderNumber = v.findViewById(R.id.tvOrderNumber);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvRestaurant = v.findViewById(R.id.tvRestaurant);
            tvRestaurantAddress = v.findViewById(R.id.tvRestaurantAddress);
            tvCustomerName = v.findViewById(R.id.tvCustomerName);
            tvDeliveryAddress = v.findViewById(R.id.tvDeliveryAddress);
            tvCompletedDate = v.findViewById(R.id.tvCompletedDate);
        }
    }
}
