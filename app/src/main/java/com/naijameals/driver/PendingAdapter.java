package com.naijameals.driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naijameals.driver.api.models.Delivery;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.VH> {
    private final List<Delivery> items;
    private final Map<Integer, PendingOrdersActivity.DistanceInfo> distances;
    private final OnActionListener onAccept;
    private final OnActionListener onDecline;

    interface OnActionListener { void onAction(int deliveryId); }

    PendingAdapter(List<Delivery> items, Map<Integer, PendingOrdersActivity.DistanceInfo> distances,
                   OnActionListener onAccept, OnActionListener onDecline) {
        this.items = items;
        this.distances = distances;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Delivery d = items.get(pos);
        PendingOrdersActivity.DistanceInfo dist = distances.get(d.id);

        double price = 0;
        try { price = d.deliveryPrice != null ? Double.parseDouble(d.deliveryPrice) : 0; } catch (Exception e) {}

        h.tvOrderNumber.setText("Order #" + (d.orderNumber != null ? d.orderNumber : d.id));
        h.tvPrice.setText(String.format(Locale.UK, "£%.2f", price));

        if (d.restaurant != null) {
            h.tvRestaurant.setVisibility(View.VISIBLE);
            h.tvRestaurantAddress.setVisibility(View.VISIBLE);
            h.pickupDistanceContainer.setVisibility(View.VISIBLE);
            h.tvRestaurant.setText(d.restaurant.name);
            h.tvRestaurantAddress.setText(d.restaurant.address + ", " + d.restaurant.postcode);
            String pd = (dist != null && !dist.pickupLoading && hasDistance(dist.pickupDist, dist.pickupDur))
                    ? (dist.pickupDist + " • " + dist.pickupDur) : (dist != null && dist.pickupLoading ? "Loading..." : "—");
            h.tvPickupDistance.setText("Distance to Pickup: " + pd);
        } else {
            h.tvRestaurant.setVisibility(View.GONE);
            h.tvRestaurantAddress.setVisibility(View.GONE);
            h.pickupDistanceContainer.setVisibility(View.GONE);
        }

        h.tvCustomerName.setText(d.customerName != null ? d.customerName : "");
        h.tvCustomerName.setVisibility(d.customerName != null ? View.VISIBLE : View.GONE);
        h.tvDeliveryAddress.setText(d.address + ", " + d.postcode);
        String dd = (dist != null && !dist.dropoffLoading && hasDistance(dist.dropoffDist, dist.dropoffDur))
                ? (dist.dropoffDist + " • " + dist.dropoffDur) : (dist != null && dist.dropoffLoading ? "Loading..." : "—");
        h.tvDropoffDistance.setText("Distance to Drop-off: " + dd);

        if (d.note != null && !d.note.isEmpty()) {
            h.noteContainer.setVisibility(View.VISIBLE);
            h.tvNote.setText(d.note);
        } else {
            h.noteContainer.setVisibility(View.GONE);
        }

        h.btnAccept.setOnClickListener(v -> onAccept.onAction(d.id));
        h.btnDecline.setOnClickListener(v -> onDecline.onAction(d.id));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static boolean hasDistance(String dist, String dur) {
        return (dist != null && !dist.isEmpty() && !dist.equals("—"))
                || (dur != null && !dur.isEmpty() && !dur.equals("—"));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderNumber, tvStatus, tvPrice, tvRestaurant, tvRestaurantAddress, tvPickupDistance;
        TextView tvCustomerName, tvDeliveryAddress, tvDropoffDistance, tvNote;
        View pickupDistanceContainer, noteContainer;
        Button btnAccept, btnDecline;

        VH(View v) {
            super(v);
            tvOrderNumber = v.findViewById(R.id.tvOrderNumber);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvRestaurant = v.findViewById(R.id.tvRestaurant);
            tvRestaurantAddress = v.findViewById(R.id.tvRestaurantAddress);
            pickupDistanceContainer = v.findViewById(R.id.pickupDistanceContainer);
            tvPickupDistance = v.findViewById(R.id.tvPickupDistance);
            tvCustomerName = v.findViewById(R.id.tvCustomerName);
            tvDeliveryAddress = v.findViewById(R.id.tvDeliveryAddress);
            tvDropoffDistance = v.findViewById(R.id.tvDropoffDistance);
            noteContainer = v.findViewById(R.id.noteContainer);
            tvNote = v.findViewById(R.id.tvNote);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
        }
    }
}
