package com.portfolio.simulator.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Request for the all-scenarios endpoint.
 *
 * The caller provides a single stock market allocation percentage (SMA).
 * The full portfolio breakdown is derived automatically:
 *   Stocks (SMA):  56% CRSP 1-10, 10% CRSP 6-10, 23% F/F Intl, 11% F/F Emg Mkts
 *   REIT:          lesser of 10% or remaining after stocks
 *   Bonds:         remaining split equally between 1-mo T-Bills and 5-yr Treasuries
 */
public class AllScenariosRequest {

    @Positive(message = "Starting nest egg must be positive")
    private double startingNestEgg = 1_000_000.0;

    @Positive(message = "Initial withdrawal must be positive")
    private double initialWithdrawal = 40_000.0;

    /** Percentage of portfolio in globally diversified stocks (0.0 – 1.0). Default 60%. */
    @DecimalMin(value = "0.0", message = "Stock market allocation cannot be negative")
    @DecimalMax(value = "1.0", message = "Stock market allocation cannot exceed 100%")
    private double stockMarketAllocation = 0.60;

    /** Number of years to simulate per scenario. Default 40. */
    @Min(value = 1, message = "Year count must be at least 1")
    private int yearCount = 40;

    @DecimalMin(value = "0.0", message = "Expenses fee cannot be negative")
    @DecimalMax(value = "0.10", message = "Expenses fee cannot exceed 10%")
    private double expensesAndMgmtFee = 0.012;

    // -------------------------------------------------------------------------
    // Primary getters & setters
    // -------------------------------------------------------------------------

    public double getStartingNestEgg() { return startingNestEgg; }
    public void setStartingNestEgg(double startingNestEgg) { this.startingNestEgg = startingNestEgg; }

    public double getInitialWithdrawal() { return initialWithdrawal; }
    public void setInitialWithdrawal(double initialWithdrawal) { this.initialWithdrawal = initialWithdrawal; }

    public double getStockMarketAllocation() { return stockMarketAllocation; }
    public void setStockMarketAllocation(double stockMarketAllocation) { this.stockMarketAllocation = stockMarketAllocation; }

    public int getYearCount() { return yearCount; }
    public void setYearCount(int yearCount) { this.yearCount = yearCount; }

    public double getExpensesAndMgmtFee() { return expensesAndMgmtFee; }
    public void setExpensesAndMgmtFee(double expensesAndMgmtFee) { this.expensesAndMgmtFee = expensesAndMgmtFee; }

    // -------------------------------------------------------------------------
    // Derived allocation getters
    // Stock slice: CRSP 1-10 (56%), CRSP 6-10 (10%), F/F Intl (23%), F/F Emg Mkts (11%)
    // REIT: lesser of 10% or non-stock remainder
    // Bonds: remainder split 50/50 between 1-mo T-Bills and 5-yr Treasuries
    // S&P 500: always 0 (CRSP 1-10 covers the total market)
    // -------------------------------------------------------------------------

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
}
