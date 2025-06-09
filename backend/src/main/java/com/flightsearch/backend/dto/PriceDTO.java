package com.flightsearch.backend.dto;

public class PriceDTO {
    private String base;
    private String total;
    private String fees;
    private String pricePerAdult;
    private String currency;

    public PriceDTO() {}
    public PriceDTO(String base, String total, String fees, String pricePerAdult, String currency) {
        this.base = base;
        this.total = total;
        this.fees = fees;
        this.pricePerAdult = pricePerAdult;
    }
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }
    public String getFees() { return fees; }
    public void setFees(String fees) { this.fees = fees; }
    public String getPricePerAdult() { return pricePerAdult; }
    public void setPricePerAdult(String pricePerAdult) { this.pricePerAdult = pricePerAdult; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}

