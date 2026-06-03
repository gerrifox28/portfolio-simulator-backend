package com.portfolio.simulator.model;

/**
 * One simulated year of output.
 * Maps to a single row in the "Calcs and Formulas" sheet.
 */
public class YearResult {

    /** Row sequence number (1, 2, 3, …) */
    private int sequenceNumber;

    /** Calendar year */
    private int year;

    /** CPI / inflation rate for this year (e.g. 0.03 = 3%) */
    private double inflation;

    /** Portfolio value at the start of this year */
    private double portfolioBeginning;

    /** Annual withdrawal taken at the start of this year */
    private double annualWithdrawal;

    /** Blended portfolio return rate for this year */
    private double portfolioReturnRate;

    /** Dollar gain/loss: rate × (beginning − withdrawal) */
    private double portfolioReturnDollars;

    /** Total income drawn (portfolio withdrawal + annuity payment) */
    private double totalIncome;

    /** Portfolio value at end of year: beginning − withdrawal + gain */
    private double portfolioEnd;

    /** Annuity payment received this year (0 for non-annuity simulations) */
    private double annuityPayment;

    /** Inflation adjustment % applied to annuity this year, subject to cap (0 for year 1 and non-annuity) */
    private double inflationAdjPct;

    // --- Getters & Setters ---

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getInflation() { return inflation; }
    public void setInflation(double inflation) { this.inflation = inflation; }

    public double getPortfolioBeginning() { return portfolioBeginning; }
    public void setPortfolioBeginning(double portfolioBeginning) { this.portfolioBeginning = portfolioBeginning; }

    public double getAnnualWithdrawal() { return annualWithdrawal; }
    public void setAnnualWithdrawal(double annualWithdrawal) { this.annualWithdrawal = annualWithdrawal; }

    public double getPortfolioReturnRate() { return portfolioReturnRate; }
    public void setPortfolioReturnRate(double portfolioReturnRate) { this.portfolioReturnRate = portfolioReturnRate; }

    public double getPortfolioReturnDollars() { return portfolioReturnDollars; }
    public void setPortfolioReturnDollars(double portfolioReturnDollars) { this.portfolioReturnDollars = portfolioReturnDollars; }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getPortfolioEnd() { return portfolioEnd; }
    public void setPortfolioEnd(double portfolioEnd) { this.portfolioEnd = portfolioEnd; }

    public double getAnnuityPayment() { return annuityPayment; }
    public void setAnnuityPayment(double annuityPayment) { this.annuityPayment = annuityPayment; }

    public double getInflationAdjPct() { return inflationAdjPct; }
    public void setInflationAdjPct(double inflationAdjPct) { this.inflationAdjPct = inflationAdjPct; }
}
