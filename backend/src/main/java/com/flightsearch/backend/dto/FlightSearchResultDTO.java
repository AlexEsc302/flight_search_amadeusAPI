package com.flightsearch.backend.dto;

import java.util.List;

public class FlightSearchResultDTO {
    private String id;
    private String parentOfferId; // Id if there are return date
    private String departureDateTime;
    private String arrivalDateTime;

    private AirportDTO departureAirport;
    private AirportDTO arrivalAirport;

    private AirlineDTO airline;
    private AirlineDTO operatingAirline;

    private String duration;

    private List<FlightSegmentDTO> segments;
    private List<StopDTO> stops;

    private PriceDTO price;
    private int numberOfAdults;

    public FlightSearchResultDTO() {}

    public FlightSearchResultDTO(String id, String parentOfferId, String departureDateTime, String arrivalDateTime, AirportDTO departureAirport,
                                  AirportDTO arrivalAirport, AirlineDTO airline, AirlineDTO operatingAirline,
                                  String duration, List<FlightSegmentDTO> segments, List<StopDTO> stops,
                                  PriceDTO price, int numberOfAdults) {
        this.id = id;
        this.parentOfferId = parentOfferId;
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.airline = airline;
        this.operatingAirline = operatingAirline;
        this.duration = duration;
        this.segments = segments;
        this.stops = stops;
        this.price = price;
        this.numberOfAdults = numberOfAdults;
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getParentOfferId() { return parentOfferId; }
    public void setParentOfferId(String parentOfferId) { this.parentOfferId = parentOfferId; }

    public String getDepartureDateTime() { return departureDateTime; }
    public void setDepartureDateTime(String departureDateTime) { this.departureDateTime = departureDateTime; }

    public String getArrivalDateTime() { return arrivalDateTime; }
    public void setArrivalDateTime(String arrivalDateTime) { this.arrivalDateTime = arrivalDateTime; }

    public AirportDTO getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(AirportDTO departureAirport) { this.departureAirport = departureAirport; }

    public AirportDTO getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(AirportDTO arrivalAirport) { this.arrivalAirport = arrivalAirport; }

    public AirlineDTO getAirline() { return airline; }
    public void setAirline(AirlineDTO airline) { this.airline = airline; }

    public AirlineDTO getOperatingAirline() { return operatingAirline; }
    public void setOperatingAirline(AirlineDTO operatingAirline) { this.operatingAirline = operatingAirline; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public List<FlightSegmentDTO> getSegments() { return segments; }
    public void setSegments(List<FlightSegmentDTO> segments) { this.segments = segments; }

    public List<StopDTO> getStops() { return stops; }
    public void setStops(List<StopDTO> stops) { this.stops = stops; }

    public PriceDTO getPrice() { return price; }
    public void setPrice(PriceDTO price) { this.price = price; }

    public int getNumberOfAdults() { return numberOfAdults; }
    public void setNumberOfAdults(int numberOfAdults) { this.numberOfAdults = numberOfAdults; }
}
