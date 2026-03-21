package com.naijameals.driver.api.models;

import com.google.gson.annotations.SerializedName;

public class Delivery {
    @SerializedName("id")
    public int id;

    @SerializedName("order_number")
    public String orderNumber;

    @SerializedName("restaurant")
    public Restaurant restaurant;

    @SerializedName("address")
    public String address;

    @SerializedName("postcode")
    public String postcode;

    @SerializedName("customer_name")
    public String customerName;

    @SerializedName("customer_mobile")
    public String customerMobile;

    @SerializedName("note")
    public String note;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;

    @SerializedName("status")
    public String status;

    @SerializedName("delivery_price")
    public String deliveryPrice;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;
}
