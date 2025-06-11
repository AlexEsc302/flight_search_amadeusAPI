package com.flightsearch.backend.dto;

import java.util.List;

public class DetailedSegmentDTO {
    private String departureIataCode;
    private String arrivalIataCode;
    private String departureDateTime;
    private String arrivalDateTime;
    private String carrierCode;
    private String number;
    private String duration;
    private String operatingCarrierCode;
    private String aircraftCode;

    // New fields
    private String departureAirportName; // From AirportDTO name in search response, or map.
    private String arrivalAirportName;   // From AirportDTO name in search response, or map.
    private String airlineName;        // Name of the main airline (e.g., "SPIRIT AIRLINES")
    private String operatingAirlineName; // Name of the operating airline (if different)
    private String aircraftTypeName;   // e.g., "AIRBUS A320"

    // To hold fare details specific to this segment for each traveler
    private List<FareDetailDTO> travelerFareDetails;


    // Getters and Setters (add for new fields, keep for existing)
    public String getDepartureIataCode() {
        return departureIataCode;
    }

    public void setDepartureIataCode(String departureIataCode) {
        this.departureIataCode = departureIataCode;
    }

    public String getArrivalIataCode() {
        return arrivalIataCode;
    }

    public void setArrivalIataCode(String arrivalIataCode) {
        this.arrivalIataCode = arrivalIataCode;
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

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getOperatingCarrierCode() {
        return operatingCarrierCode;
    }

    public void setOperatingCarrierCode(String operatingCarrierCode) {
        this.operatingCarrierCode = operatingCarrierCode;
    }

    public String getAircraftCode() {
        return aircraftCode;
    }

    public void setAircraftCode(String aircraftCode) {
        this.aircraftCode = aircraftCode;
    }

    // New Getters and Setters
    public String getDepartureAirportName() {
        return departureAirportName;
    }

    public void setDepartureAirportName(String departureAirportName) {
        this.departureAirportName = departureAirportName;
    }

    public String getArrivalAirportName() {
        return arrivalAirportName;
    }

    public void setArrivalAirportName(String arrivalAirportName) {
        this.arrivalAirportName = arrivalAirportName;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getOperatingAirlineName() {
        return operatingAirlineName;
    }

    public void setOperatingAirlineName(String operatingAirlineName) {
        this.operatingAirlineName = operatingAirlineName;
    }

    public String getAircraftTypeName() {
        return aircraftTypeName;
    }

    public void setAircraftTypeName(String aircraftTypeName) {
        this.aircraftTypeName = aircraftTypeName;
    }

    public List<FareDetailDTO> getTravelerFareDetails() {
        return travelerFareDetails;
    }

    public void setTravelerFareDetails(List<FareDetailDTO> travelerFareDetails) {
        this.travelerFareDetails = travelerFareDetails;
    }
}