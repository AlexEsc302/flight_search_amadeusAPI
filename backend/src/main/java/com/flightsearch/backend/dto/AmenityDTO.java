// src/main/java/com/flightsearch/backend/dto/AmenityDTO.java
package com.flightsearch.backend.dto;

public class AmenityDTO {
    private String description;
    private boolean isChargeable;
    private String amenityType;
    // Getters y Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isChargeable() { return isChargeable; }
    public void setChargeable(boolean chargeable) { isChargeable = chargeable; }

    public String getAmenityType() { return amenityType; }
    public void setAmenityType(String amenityType) { this.amenityType = amenityType; }

}