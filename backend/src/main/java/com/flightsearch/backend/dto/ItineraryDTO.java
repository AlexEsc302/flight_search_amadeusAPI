package com.flightsearch.backend.dto;

import java.util.List;
 
public class ItineraryDTO {
    private String id; 
    private String direction; 
    private String duration; // Overall itinerary duration (e.g., "PT6H15M")
    private String departureDateTime;
    private String arrivalDateTime;
    private AirportDTO departureAirport; // Using AirportDTO for name and code
    private AirportDTO arrivalAirport;   // Using AirportDTO for name and code
    private List<StopDTO> stops; // List of stopovers with their details

    // Existing segments list
    private List<DetailedSegmentDTO> segments;

    // Getters and Setters 
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public List<DetailedSegmentDTO> getSegments() {
        return segments;
    }

    public void setSegments(List<DetailedSegmentDTO> segments) {
        this.segments = segments;
    }

    public String getDepartureDateTime() {
        return departureDateTime;
    }

    public void setDepartureDateTime(String departureDateTime) {
        this.departureDateTime = departureDateTime;
    }

    public String getArrivalDateTime() {
        return arrivalDateTime;
    }

    public void setArrivalDateTime(String arrivalDateTime) {
        this.arrivalDateTime = arrivalDateTime;
    }

    public AirportDTO getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(AirportDTO departureAirport) {
        this.departureAirport = departureAirport;
    }

    public AirportDTO getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(AirportDTO arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public List<StopDTO> getStops() {
        return stops;
    }

    public void setStops(List<StopDTO> stops) {
        this.stops = stops;
    }
}