package com.naijameals.driver.api.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("email")
    public String email;

    @SerializedName("mobile")
    public String mobile;

    @SerializedName("house_address")
    public String houseAddress;

    @SerializedName("postcode")
    public String postcode;

    @SerializedName("delivery_type")
    public String deliveryType;

    @SerializedName("isOnline")
    public boolean isOnline;

    @SerializedName("isConfirmed")
    public boolean isConfirmed;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;

    @SerializedName("newOrder")
    public Boolean newOrder;
}
