package com.portfolio.simulator.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Request for POST /api/simulate/compare.
 *
 * Runs two all-scenarios simulations — one without an annuity and one with —
 * and returns both results for side-by-side comparison.
 */
public class AnnuityCompareRequest {

    @Positive(message = "Starting nest egg must be positive")
    private double startingNestEgg = 1_000_000.0;

    @Positive(message = "Initial withdrawal must be positive")
    private double initialWithdrawal = 40_000.0;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double stockMarketAllocation = 0.60;

    @Min(1)
    private int yearCount = 40;

    @DecimalMin("0.0") @DecimalMax("0.10")
    private double expensesAndMgmtFee = 0.012;

    // ── Annuity inputs ────────────────────────────────────────────────────────

    /** Age at which the annuity is purchased (49–80). */
    @Min(value = 49, message = "Age must be at least 49")
    @Max(value = 80, message = "Age cannot exceed 80")
    private int age = 65;

    /** true = joint-life coverage, false = single-life. */
    private boolean joint = false;

    /** Fraction of the nest egg used to purchase the annuity (0.0–1.0). */
    @DecimalMin(value = "0.0", message = "Annuity percentage cannot be negative")
    @DecimalMax(value = "1.0", message = "Annuity percentage cannot exceed 100%")
    private double annuityPercentage = 0.30;

    /** "inflation_adjusted" (default) or "fixed". */
    private String withdrawalMode = "inflation_adjusted";

    /** Annual cap on annuity COLA adjustment. Default 3%. */
    @DecimalMin("0.0") @DecimalMax("1.0")
    private double annuityCap = 0.03;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public double getStartingNestEgg() { return startingNestEgg; }
    public void setStartingNestEgg(double v) { this.startingNestEgg = v; }

    public double getInitialWithdrawal() { return initialWithdrawal; }
    public void setInitialWithdrawal(double v) { this.initialWithdrawal = v; }

    public double getStockMarketAllocation() { return stockMarketAllocation; }
    public void setStockMarketAllocation(double v) { this.stockMarketAllocation = v; }

    public int getYearCount() { return yearCount; }
    public void setYearCount(int v) { this.yearCount = v; }

    public double getExpensesAndMgmtFee() { return expensesAndMgmtFee; }
    public void setExpensesAndMgmtFee(double v) { this.expensesAndMgmtFee = v; }

    public int getAge() { return age; }
    public void setAge(int v) { this.age = v; }

    public boolean isJoint() { return joint; }
    public void setJoint(boolean v) { this.joint = v; }

    public double getAnnuityPercentage() { return annuityPercentage; }
    public void setAnnuityPercentage(double v) { this.annuityPercentage = v; }

    public String getWithdrawalMode() { return withdrawalMode; }
    public void setWithdrawalMode(String v) { this.withdrawalMode = v; }

    public double getAnnuityCap() { return annuityCap; }
    public void setAnnuityCap(double v) { this.annuityCap = v; }

    // ── Derived allocation getters (same formula as AllScenariosRequest) ──────

    public double getSp500()      { return 0.0; }
    public double getCrsp1_10()   { return stockMarketAllocation * 0.56; }
    public double getCrsp6_10()   { return stockMarketAllocation * 0.10; }
    public double getFfIntl()     { return stockMarketAllocation * 0.23; }
    public double getFfEmgMkts()  { return stockMarketAllocation * 0.11; }

    public double getDjUsReit() {
        return Math.min(0.10, 1.0 - stockMarketAllocation);
    }

    private double bondAlloc() {
        return (1.0 - stockMarketAllocation - getDjUsReit()) / 2.0;
    }

    public double getOneMonth()   { return bondAlloc(); }
    public double getFiveYearUS() { return bondAlloc(); }

    /** Converts this request to an AllScenariosRequest for the without-annuity run. */
    public AllScenariosRequest toAllScenariosRequest() {
        AllScenariosRequest r = new AllScenariosRequest();
        r.setStartingNestEgg(startingNestEgg);
        r.setInitialWithdrawal(initialWithdrawal);
        r.setStockMarketAllocation(stockMarketAllocation);
        r.setYearCount(yearCount);
        r.setExpensesAndMgmtFee(expensesAndMgmtFee);
        r.setWithdrawalMode(withdrawalMode);
        return r;
    }
}
