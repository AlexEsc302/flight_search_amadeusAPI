package com.flightsearch.backend.dto;

import java.util.List;
 
public class ItineraryDTO {
    // Existing fields
    private String id; // Optional: If you want to give a unique ID to each itinerary within an offer
    private String direction; // Optional: "OUTBOUND" or "INBOUND" for roundtrips
    private String duration; // Overall itinerary duration (e.g., "PT6H15M")

    // New fields for overall itinerary summary
    private String departureDateTime;
    private String arrivalDateTime;
    private AirportDTO departureAirport; // Using AirportDTO for name and code
    private AirportDTO arrivalAirport;   // Using AirportDTO for name and code
    private List<StopDTO> stops; // List of stopovers with their details

    // Existing segments list
    private List<DetailedSegmentDTO> segments;

    // Getters and Setters (add for new fields, keep for existing)
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

    // New Getters and Setters
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