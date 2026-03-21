package com.naijameals.driver.api.models;

import com.google.gson.annotations.SerializedName;

public class Restaurant {
    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("address")
    public String address;

    @SerializedName("postcode")
    public String postcode;

    @SerializedName("mobile")
    public String mobile;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;
}
