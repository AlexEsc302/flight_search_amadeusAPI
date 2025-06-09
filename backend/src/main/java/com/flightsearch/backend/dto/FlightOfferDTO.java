package com.flightsearch.backend.dto;

import java.util.List;

public class FlightOfferDTO {
    private PriceDTO price;
    private List<List<FlightSegmentDTO>> itineraries;

    public FlightOfferDTO() {}

    public FlightOfferDTO(PriceDTO price, List<List<FlightSegmentDTO>> itineraries) {
        this.price = price;
        this.itineraries = itineraries;
    }

    public PriceDTO getPrice() {
        return price;
    }

    public void setPrice(PriceDTO price) {
        this.price = price;
    }

    public List<List<FlightSegmentDTO>> getItineraries() {
        return itineraries;
    }

    public void setItineraries(List<List<FlightSegmentDTO>> itineraries) {
        this.itineraries = itineraries;
    }
}
