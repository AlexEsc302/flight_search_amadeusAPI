package com.flightsearch.backend.dto;

public class StopDTO { 
    private AirportDTO airport; 
    private String duration;

    public StopDTO() {}

    public StopDTO(AirportDTO airport, String duration) {
        this.airport = airport;
        this.duration = duration;
    }

    public AirportDTO getAirport() { return airport; }
    public void setAirport(AirportDTO airport) { this.airport = airport; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
}