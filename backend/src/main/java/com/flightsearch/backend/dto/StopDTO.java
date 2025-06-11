// src/main/java/com/flightsearch/backend/dto/StopDTO.java
package com.flightsearch.backend.dto;

public class StopDTO {
    private String airportCode;
    private String airportName;
    private String layoverDuration; // e.g., "PT2H40M" or "2h 40m"

    // Getters and Setters
    public String getAirportCode() {
        return airportCode;
    }

    public void setAirportCode(String airportCode) {
        this.airportCode = airportCode;
    }

    public String getAirportName() {
        return airportName;
    }

    public void setAirportName(String airportName) {
        this.airportName = airportName;
    }

    public String getLayoverDuration() {
        return layoverDuration;
    }

    public void setLayoverDuration(String layoverDuration) {
        this.layoverDuration = layoverDuration;
    }
}