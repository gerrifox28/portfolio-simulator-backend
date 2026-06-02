package com.portfolio.simulator.model;

public class CashFlow {
    private String id;
    private String description;
    private double amount;
    private boolean allYears;
    private Integer year; // 1-indexed simulation sequence year; null when allYears === true

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public boolean isAllYears() { return allYears; }
    public void setAllYears(boolean allYears) { this.allYears = allYears; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
}
