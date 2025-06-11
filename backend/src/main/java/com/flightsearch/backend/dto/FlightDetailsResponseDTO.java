package com.flightsearch.backend.dto;

import java.util.List;

public class FlightDetailsResponseDTO {
    private String amadeusOfferId; // The original Amadeus Offer ID that was detailed
    private PriceDTO totalPrice; 
    private int numberOfAdults;
    private List<ItineraryDTO> itineraries; // Will contain outbound and inbound itineraries

    public FlightDetailsResponseDTO() {
    }

    // Getters and Setters
    public String getAmadeusOfferId() {
        return amadeusOfferId;
    }
 
    public void setAmadeusOfferId(String amadeusOfferId) {
        this.amadeusOfferId = amadeusOfferId;
    }

    public PriceDTO getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(PriceDTO totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getNumberOfAdults() {
        return numberOfAdults;
    }

    public void setNumberOfAdults(int numberOfAdults) {
        this.numberOfAdults = numberOfAdults;
    }

    public List<ItineraryDTO> getItineraries() {
        return itineraries;
    }

    public void setItineraries(List<ItineraryDTO> itineraries) {
        this.itineraries = itineraries;
    }
}