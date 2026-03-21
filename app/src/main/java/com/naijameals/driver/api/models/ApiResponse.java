package com.naijameals.driver.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiResponse<T> {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("token")
    public String token;

    @SerializedName("user")
    public User user;

    @SerializedName("deliveries")
    public List<Delivery> deliveries;

    @SerializedName("weekly_earnings")
    public Double weeklyEarnings;

    @SerializedName("delivery")
    public Delivery delivery;

    @SerializedName("data")
    public T data;

    @SerializedName("distance")
    public String distance;

    @SerializedName("duration")
    public String duration;

    @SerializedName("error")
    public String error;
}
