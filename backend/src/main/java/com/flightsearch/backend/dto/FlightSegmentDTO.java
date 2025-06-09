package com.flightsearch.backend.dto;

public class FlightSegmentDTO {
    private String departureIataCode;
    private String arrivalIataCode;
    private String departureDateTime;
    private String arrivalDateTime;
    private String carrierCode;
    private String number;
    private String duration;
    private String operatingCarrierCode; // <-- ADD THIS FIELD

    public FlightSegmentDTO() {}

    public FlightSegmentDTO(String departureIataCode, String arrivalIataCode, String departureDateTime,
                            String arrivalDateTime, String carrierCode, String number, String duration,
                            String operatingCarrierCode) { // <-- ADD TO CONSTRUCTOR
        this.departureIataCode = departureIataCode;
        this.arrivalIataCode = arrivalIataCode;
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
        this.carrierCode = carrierCode;
        this.number = number;
        this.duration = duration;
        this.operatingCarrierCode = operatingCarrierCode; // <-- SET IN CONSTRUCTOR
    }

    public String getDepartureIataCode() { return departureIataCode; }
    public void setDepartureIataCode(String departureIataCode) { this.departureIataCode = departureIataCode; }

    public String getArrivalIataCode() { return arrivalIataCode; }
    public void setArrivalIataCode(String arrivalIataCode) { this.arrivalIataCode = arrivalIataCode; }

    public String getDepartureDateTime() { return departureDateTime; }
    public void setDepartureDateTime(String departureDateTime) { this.departureDateTime = departureDateTime; }

    public String getArrivalDateTime() { return arrivalDateTime; }
    public void setArrivalDateTime(String arrivalDateTime) { this.arrivalDateTime = arrivalDateTime; }

    public String getCarrierCode() { return carrierCode; }
    public void setCarrierCode(String carrierCode) { this.carrierCode = carrierCode; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getOperatingCarrierCode() { return operatingCarrierCode; }
    public void setOperatingCarrierCode(String operatingCarrierCode) { this.operatingCarrierCode = operatingCarrierCode; }
}