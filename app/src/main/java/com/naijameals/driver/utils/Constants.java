package com.naijameals.driver.utils;

/**
 * App Constants - same as React app constants.ts
 * Testing: For physical device, replace 127.0.0.1 with your computer's IP (run ipconfig to find it).
 * Emulator: Use 10.0.2.2 instead of 127.0.0.1 to reach host machine.
 */
public final class Constants {
    public static final String API_BASE_URL = "https://api.naijameals.com/api";
    public static final String TOKEN_KEY = "@driver_token";
    public static final String USER_KEY = "@driver_user";
    public static final String[] DELIVERY_TYPES = {"Car", "Motorcycle", "Bicycle"};
    public static final long LOCATION_UPDATE_INTERVAL_MS = 30000; // 30 seconds
}
