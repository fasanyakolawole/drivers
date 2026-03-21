package com.naijameals.driver.api;

import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.api.models.CalculateEtaRequest;
import com.naijameals.driver.api.models.Delivery;
import com.naijameals.driver.api.models.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Retrofit API interface - all endpoints from React app
 */
public interface DriverApi {

    // Auth - Public
    @POST("driver/register")
    Call<ApiResponse<Object>> register(@Body Map<String, Object> data);

    @POST("driver/login")
    Call<ApiResponse<Object>> login(@Body Map<String, Object> data);

    // Auth - Protected
    @POST("driver/logout")
    Call<ApiResponse<Object>> logout(@Body Map<String, Object> body);

    @GET("driver/profile")
    Call<ApiResponse<Object>> getProfile();

    @PUT("driver/profile")
    Call<ApiResponse<Object>> updateProfile(@Body Map<String, Object> data);

    // Deliveries
    @GET("driver/deliveries")
    Call<ApiResponse<Object>> getDeliveries();

    @GET("driver/deliveries/{id}")
    Call<ApiResponse<Object>> getDelivery(@Path("id") int id);

    @POST("driver/deliveries/{id}/accept")
    Call<ApiResponse<Object>> acceptDelivery(@Path("id") int id, @Body Map<String, Object> body);

    @POST("driver/deliveries/{id}/decline")
    Call<ApiResponse<Object>> declineDelivery(@Path("id") int id, @Body Map<String, Object> body);

    @PUT("driver/deliveries/{id}/status")
    Call<ApiResponse<Object>> updateDeliveryStatus(@Path("id") int id, @Body Map<String, Object> body);

    @PUT("driver/deliveries/{id}/complete")
    Call<ApiResponse<Object>> completeDelivery(@Path("id") int id, @Body Map<String, Object> body);

    // Location
    @PUT("driver/location")
    Call<ApiResponse<Object>> updateLocation(@Body Map<String, Object> body);

    @PUT("driver/toggle-online")
    Call<ApiResponse<Object>> toggleOnline(@Body Map<String, Object> body);

    // ETA - POST /api/driver/deliveries/calculate-eta
    @POST("driver/deliveries/calculate-eta")
    Call<ApiResponse<Object>> calculateEta(@Body CalculateEtaRequest body);
}
