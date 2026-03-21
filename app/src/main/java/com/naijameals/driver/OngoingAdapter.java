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

public class OngoingAdapter extends RecyclerView.Adapter<OngoingAdapter.VH> {
    private final List<Delivery> items;
    private final OnMapsListener onMaps;
    private final OnCallListener onCall;
    private final OnActionListener onPickedUp;
    private final OnActionListener onComplete;

    interface OnMapsListener { void onMaps(String address); }
    interface OnCallListener { void onCall(String number); }
    interface OnActionListener { void onAction(int deliveryId); }

    OngoingAdapter(List<Delivery> items, OnMapsListener onMaps, OnCallListener onCall,
                  OnActionListener onPickedUp, OnActionListener onComplete) {
        this.items = items;
        this.onMaps = onMaps;
        this.onCall = onCall;
        this.onPickedUp = onPickedUp;
        this.onComplete = onComplete;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ongoing_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Delivery d = items.get(pos);
        double price = 0;
        try { price = d.deliveryPrice != null ? Double.parseDouble(d.deliveryPrice) : 0; } catch (Exception e) {}

        h.tvOrderNumber.setText("Order #" + (d.orderNumber != null ? d.orderNumber : d.id));
        h.tvStatus.setText(getStatusText(d.status));
        h.tvPrice.setText(String.format(Locale.UK, "£%.2f", price));

        if (d.restaurant != null) {
            h.tvRestaurant.setText(d.restaurant.name);
            String addr = d.restaurant.address + ", " + d.restaurant.postcode;
            h.tvRestaurantAddress.setText(addr);
            h.btnNavPickup.setOnClickListener(v -> onMaps.onMaps(addr));
            if (d.restaurant.mobile != null && !d.restaurant.mobile.isEmpty()) {
                h.rowRestaurantPhone.setVisibility(View.VISIBLE);
                h.tvRestaurantPhone.setText(d.restaurant.mobile);
                h.rowRestaurantPhone.setOnClickListener(v -> onCall.onCall(d.restaurant.mobile));
            } else {
                h.rowRestaurantPhone.setVisibility(View.GONE);
            }
        } else {
            h.rowRestaurantPhone.setVisibility(View.GONE);
        }

        h.tvCustomerName.setText(d.customerName != null ? d.customerName : "");
        h.tvCustomerName.setVisibility(d.customerName != null ? View.VISIBLE : View.GONE);
        String dropAddr = d.address + ", " + d.postcode;
        h.tvDeliveryAddress.setText(dropAddr);
        h.btnNavDropoff.setOnClickListener(v -> onMaps.onMaps(dropAddr));
        if (d.customerMobile != null && !d.customerMobile.isEmpty()) {
            h.rowCustomerPhone.setVisibility(View.VISIBLE);
            h.tvCustomerPhone.setText(d.customerMobile);
            h.rowCustomerPhone.setOnClickListener(v -> onCall.onCall(d.customerMobile));
        } else {
            h.rowCustomerPhone.setVisibility(View.GONE);
        }

        if (d.note != null && !d.note.isEmpty()) {
            h.noteContainer.setVisibility(View.VISIBLE);
            h.tvNote.setText(d.note);
        } else {
            h.noteContainer.setVisibility(View.GONE);
        }

        h.statusBadgeContainer.setBackgroundResource(getStatusBadgeRes(d.status));

        h.layoutPickingUp.setVisibility("picking_up".equals(d.status) ? View.VISIBLE : View.GONE);
        h.layoutInProgress.setVisibility("in_progress".equals(d.status) ? View.VISIBLE : View.GONE);
        h.layoutAssigned.setVisibility("assigned".equals(d.status) ? View.VISIBLE : View.GONE);

        h.btnPickedUp.setOnClickListener(v -> onPickedUp.onAction(d.id));
        h.btnComplete.setOnClickListener(v -> onComplete.onAction(d.id));
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "assigned": return "ASSIGNED";
            case "picking_up": return "PICKING UP";
            case "in_progress": return "IN PROGRESS";
            default: return status.replace("_", " ").toUpperCase();
        }
    }

    private int getStatusBadgeRes(String status) {
        if (status == null) return R.drawable.bg_ongoing_badge_assigned;
        switch (status) {
            case "assigned": return R.drawable.bg_ongoing_badge_assigned;
            case "picking_up": return R.drawable.bg_ongoing_badge_picking_up;
            case "in_progress": return R.drawable.bg_ongoing_badge_in_progress;
            default: return R.drawable.bg_ongoing_badge_assigned;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderNumber, tvStatus, tvPrice, tvRestaurant, tvRestaurantAddress, tvRestaurantPhone, tvCustomerName, tvDeliveryAddress, tvCustomerPhone, tvNote;
        View rowRestaurantPhone, rowCustomerPhone, statusBadgeContainer, noteContainer;
        Button btnNavPickup, btnNavDropoff, btnPickedUp, btnComplete;
        View layoutPickingUp, layoutInProgress, layoutAssigned;

        VH(View v) {
            super(v);
            tvOrderNumber = v.findViewById(R.id.tvOrderNumber);
            tvStatus = v.findViewById(R.id.tvStatus);
            statusBadgeContainer = v.findViewById(R.id.statusBadgeContainer);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvRestaurant = v.findViewById(R.id.tvRestaurant);
            tvRestaurantAddress = v.findViewById(R.id.tvRestaurantAddress);
            rowRestaurantPhone = v.findViewById(R.id.rowRestaurantPhone);
            tvRestaurantPhone = v.findViewById(R.id.tvRestaurantPhone);
            tvCustomerName = v.findViewById(R.id.tvCustomerName);
            tvDeliveryAddress = v.findViewById(R.id.tvDeliveryAddress);
            rowCustomerPhone = v.findViewById(R.id.rowCustomerPhone);
            tvCustomerPhone = v.findViewById(R.id.tvCustomerPhone);
            noteContainer = v.findViewById(R.id.noteContainer);
            tvNote = v.findViewById(R.id.tvNote);
            btnNavPickup = v.findViewById(R.id.btnNavPickup);
            btnNavDropoff = v.findViewById(R.id.btnNavDropoff);
            btnPickedUp = v.findViewById(R.id.btnPickedUp);
            btnComplete = v.findViewById(R.id.btnComplete);
            layoutPickingUp = v.findViewById(R.id.layoutPickingUp);
            layoutInProgress = v.findViewById(R.id.layoutInProgress);
            layoutAssigned = v.findViewById(R.id.layoutAssigned);
        }
    }
}
