package com.portfolio.simulator.model;

public class CashFlow {
    private String id;
    private String description;
    private double amount;
    private boolean allYears;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public boolean isAllYears() { return allYears; }
    public void setAllYears(boolean allYears) { this.allYears = allYears; }

    /** First year of the range (1-indexed); null when allYears === true */
    private Integer yearStart;
    public Integer getYearStart() { return yearStart; }
    public void setYearStart(Integer yearStart) { this.yearStart = yearStart; }

    /** Last year of the range; equals yearStart for single-year entries; null when allYears === true */
    private Integer yearEnd;
    public Integer getYearEnd() { return yearEnd; }
    public void setYearEnd(Integer yearEnd) { this.yearEnd = yearEnd; }

    /** "none" (default) | "full" | "half" — only meaningful when allYears === true */
    private String inflationAdj = "none";
    public String getInflationAdj() { return inflationAdj; }
    public void setInflationAdj(String inflationAdj) { this.inflationAdj = inflationAdj != null ? inflationAdj : "none"; }
}
