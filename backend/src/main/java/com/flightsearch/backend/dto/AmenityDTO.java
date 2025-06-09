package com.flightsearch.backend.dto;

public class AmenityDTO {
    private String name;
    private boolean chargeable;

    public AmenityDTO() {}
    public AmenityDTO(String name, boolean chargeable) {
        this.name = name;
        this.chargeable = chargeable;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isChargeable() { return chargeable; }
    public void setChargeable(boolean chargeable) { this.chargeable = chargeable; }
}