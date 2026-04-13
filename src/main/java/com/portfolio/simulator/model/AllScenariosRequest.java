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

    /** "inflation_adjusted" (default) or "fixed". */
    private String withdrawalMode = "inflation_adjusted";

    /**
     * When true, the explicit allocation fields below are used instead of the
     * derived formula. All eight values must be provided and should sum to 1.0.
     */
    private boolean manualAllocations = false;

    // Explicit allocation fields — only used when manualAllocations = true
    @DecimalMin("0.0") @DecimalMax("1.0") private double mSp500     = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mCrsp1_10  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mCrsp6_10  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mFfIntl    = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mFfEmgMkts = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mDjUsReit  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mOneMonth  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mFiveYearUS = 0.0;

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

    public String getWithdrawalMode() { return withdrawalMode; }
    public void setWithdrawalMode(String withdrawalMode) { this.withdrawalMode = withdrawalMode; }

    public boolean isManualAllocations() { return manualAllocations; }
    public void setManualAllocations(boolean v) { this.manualAllocations = v; }

    public double getMSp500()      { return mSp500; }      public void setMSp500(double v)      { this.mSp500 = v; }
    public double getMCrsp1_10()   { return mCrsp1_10; }   public void setMCrsp1_10(double v)   { this.mCrsp1_10 = v; }
    public double getMCrsp6_10()   { return mCrsp6_10; }   public void setMCrsp6_10(double v)   { this.mCrsp6_10 = v; }
    public double getMFfIntl()     { return mFfIntl; }     public void setMFfIntl(double v)     { this.mFfIntl = v; }
    public double getMFfEmgMkts()  { return mFfEmgMkts; }  public void setMFfEmgMkts(double v)  { this.mFfEmgMkts = v; }
    public double getMDjUsReit()   { return mDjUsReit; }   public void setMDjUsReit(double v)   { this.mDjUsReit = v; }
    public double getMOneMonth()   { return mOneMonth; }   public void setMOneMonth(double v)   { this.mOneMonth = v; }
    public double getMFiveYearUS() { return mFiveYearUS; } public void setMFiveYearUS(double v) { this.mFiveYearUS = v; }

    // -------------------------------------------------------------------------
    // Derived allocation getters — use explicit fields when manualAllocations=true
    // -------------------------------------------------------------------------

    public double getSp500()      { return manualAllocations ? mSp500     : 0.0; }
    public double getCrsp1_10()   { return manualAllocations ? mCrsp1_10  : stockMarketAllocation * 0.56; }
    public double getCrsp6_10()   { return manualAllocations ? mCrsp6_10  : stockMarketAllocation * 0.10; }
    public double getFfIntl()     { return manualAllocations ? mFfIntl    : stockMarketAllocation * 0.23; }
    public double getFfEmgMkts()  { return manualAllocations ? mFfEmgMkts : stockMarketAllocation * 0.11; }
    public double getDjUsReit()   { return manualAllocations ? mDjUsReit  : Math.min(0.10, 1.0 - stockMarketAllocation); }
    public double getOneMonth()   { return manualAllocations ? mOneMonth  : bondAlloc(); }
    public double getFiveYearUS() { return manualAllocations ? mFiveYearUS : bondAlloc(); }

    private double bondAlloc() {
        return (1.0 - stockMarketAllocation - Math.min(0.10, 1.0 - stockMarketAllocation)) / 2.0;
    }
}
