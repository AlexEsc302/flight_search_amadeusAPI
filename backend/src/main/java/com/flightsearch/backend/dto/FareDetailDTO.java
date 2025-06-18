package com.flightsearch.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class FareDetailDTO {
    private String cabin;
    private String fareBasis;
    private String brandedFare;
    private String classCode; 
    private List<AmenityDTO> amenities;

    public FareDetailDTO() {
        this.amenities = new ArrayList<>(); 
    }

    // Getters y Setters
    public String getCabin() { return cabin; }
    public void setCabin(String cabin) { this.cabin = cabin; }

    public String getFareBasis() { return fareBasis; }
    public void setFareBasis(String fareBasis) { this.fareBasis = fareBasis; }

    public String getBrandedFare() { return brandedFare; }
    public void setBrandedFare(String brandedFare) { this.brandedFare = brandedFare; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public List<AmenityDTO> getAmenities() { return amenities; }
    public void setAmenities(List<AmenityDTO> amenities) { this.amenities = amenities; }
}
