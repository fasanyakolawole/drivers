package com.naijameals.driver.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.naijameals.driver.api.ApiClient;
import com.naijameals.driver.api.DriverApi;
import com.naijameals.driver.api.models.ApiResponse;
import com.naijameals.driver.api.models.User;
import com.naijameals.driver.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Auth repository - same logic as React AuthService
 */
public class AuthRepository {
    private static final String PREFS_NAME = "driver_prefs";
    private final SharedPreferences prefs;
    private final Gson gson;
    private final ApiClient apiClient;

    public AuthRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        apiClient = ApiClient.getInstance();
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.TOKEN_KEY, token).apply();
    }

    public void saveUser(User user) {
        prefs.edit().putString(Constants.USER_KEY, gson.toJson(user)).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.TOKEN_KEY, null);
    }

    public User getUser() {
        String json = prefs.getString(Constants.USER_KEY, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearStorage() {
        prefs.edit().remove(Constants.TOKEN_KEY).remove(Constants.USER_KEY).apply();
    }

    public void initializeAuth(InitAuthCallback callback) {
        String token = getToken();
        User user = getUser();
        if (token != null) {
            apiClient.setToken(token);
        }
        callback.onResult(token, user);
    }

    public void register(Map<String, Object> data, AuthCallback callback) {
        DriverApi api = apiClient.getApi();
        api.register(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> body = response.body();
                    if (body.success && body.token != null && body.user != null) {
                        saveToken(body.token);
                        saveUser(body.user);
                        apiClient.setToken(body.token);
                        callback.onSuccess(body.user, body.token);
                    } else {
                        callback.onError(body.message != null ? body.message : "Registration failed");
                    }
                } else {
                    callback.onError(parseErrorResponse(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Registration failed");
            }
        });
    }

    /**
     * Parse Laravel validation/error response (422, 500, etc.)
     * Matches React ApiService error handling
     */
    private String parseErrorResponse(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                @SuppressWarnings("unchecked")
                Map<String, Object> err = gson.fromJson(json, Map.class);
                if (err != null) {
                    String msg = (String) err.get("message");
                    if (msg != null && !msg.isEmpty()) return msg;
                    // Laravel validation: errors.field = ["msg1", "msg2"]
                    Object errors = err.get("errors");
                    if (errors instanceof Map) {
                        Map<?, ?> errMap = (Map<?, ?>) errors;
                        if (!errMap.isEmpty()) {
                            Object first = errMap.values().iterator().next();
                            if (first instanceof java.util.List && !((java.util.List<?>) first).isEmpty()) {
                                return String.valueOf(((java.util.List<?>) first).get(0));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Registration failed";
    }

    public void login(String email, String password, AuthCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email.trim().toLowerCase());
        data.put("password", password);

        DriverApi api = apiClient.getApi();
        api.login(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> body = response.body();
                    if (body.success && body.token != null && body.user != null) {
                        saveToken(body.token);
                        saveUser(body.user);
                        apiClient.setToken(body.token);
                        // Refresh profile after login (same as React)
                        refreshUser(callback);
                    } else {
                        callback.onError(body.message != null ? body.message : "Login failed");
                    }
                } else {
                    callback.onError("Login failed");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Login failed");
            }
        });
    }

    public void logout(LogoutCallback callback) {
        DriverApi api = apiClient.getApi();
        Map<String, Object> body = new HashMap<>();
        api.logout(body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                clearStorage();
                apiClient.setToken(null);
                callback.onComplete();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                clearStorage();
                apiClient.setToken(null);
                callback.onComplete();
            }
        });
    }

    public void toggleOnlineStatus(Double latitude, Double longitude, String fcmToken, ToggleCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        if (latitude != null && longitude != null) {
            payload.put("latitude", latitude);
            payload.put("longitude", longitude);
        }
        if (fcmToken != null && !fcmToken.isEmpty()) {
            payload.put("fcm_token", fcmToken);
        }

        DriverApi api = apiClient.getApi();
        api.toggleOnline(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().user != null) {
                    saveUser(response.body().user);
                    callback.onSuccess(response.body().user);
                } else {
                    callback.onError(response.body() != null && response.body().message != null
                            ? response.body().message : "Failed to toggle online status");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Failed to toggle online status");
            }
        });
    }

    public void refreshUser(final AuthCallback callback) {
        DriverApi api = apiClient.getApi();
        api.getProfile().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().user != null) {
                    saveUser(response.body().user);
                    if (callback != null) callback.onSuccess(response.body().user, getToken());
                } else if (callback != null) {
                    callback.onError("Failed to refresh user");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    public interface InitAuthCallback {
        void onResult(String token, User user);
    }

    public interface AuthCallback {
        void onSuccess(User user, String token);
        void onError(String message);
    }

    public interface ToggleCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface LogoutCallback {
        void onComplete();
    }
}
