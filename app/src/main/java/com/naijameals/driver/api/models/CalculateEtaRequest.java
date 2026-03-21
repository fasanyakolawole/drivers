package com.naijameals.driver.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for calculate-eta API - matches Laravel expected format.
 * Send origin_lat/lng and destination_lat/lng as numbers (like cURL example).
 */
public class CalculateEtaRequest {
    @SerializedName("origin_lat")
    public Double originLat;

    @SerializedName("origin_lng")
    public Double originLng;

    @SerializedName("origin")
    public String origin;

    @SerializedName("destination_lat")
    public Double destinationLat;

    @SerializedName("destination_lng")
    public Double destinationLng;

    @SerializedName("destination")
    public String destination;

    public CalculateEtaRequest() {}

    public static CalculateEtaRequest fromCoords(double origLat, double origLng, double destLat, double destLng) {
        CalculateEtaRequest r = new CalculateEtaRequest();
        r.originLat = origLat;
        r.originLng = origLng;
        r.destinationLat = destLat;
        r.destinationLng = destLng;
        return r;
    }

    public static CalculateEtaRequest fromCoordsAndAddress(double origLat, double origLng, String destAddress) {
        CalculateEtaRequest r = new CalculateEtaRequest();
        r.originLat = origLat;
        r.originLng = origLng;
        r.destination = destAddress;
        return r;
    }

    public static CalculateEtaRequest fromAddressAndCoords(String origAddress, double destLat, double destLng) {
        CalculateEtaRequest r = new CalculateEtaRequest();
        r.origin = origAddress;
        r.destinationLat = destLat;
        r.destinationLng = destLng;
        return r;
    }

    public static CalculateEtaRequest fromAddresses(String origAddress, String destAddress) {
        CalculateEtaRequest r = new CalculateEtaRequest();
        r.origin = origAddress;
        r.destination = destAddress;
        return r;
    }
}
