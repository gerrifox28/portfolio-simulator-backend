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

    private boolean manualAllocations = false;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mSp500     = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mCrsp1_10  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mCrsp6_10  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mFfIntl    = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mFfEmgMkts = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mDjUsReit  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mOneMonth  = 0.0;
    @DecimalMin("0.0") @DecimalMax("1.0") private double mFiveYearUS = 0.0;

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

    // ── Derived allocation getters ────────────────────────────────────────────

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

    /** Converts this request to an AllScenariosRequest for the without-annuity run. */
    public AllScenariosRequest toAllScenariosRequest() {
        AllScenariosRequest r = new AllScenariosRequest();
        r.setStartingNestEgg(startingNestEgg);
        r.setInitialWithdrawal(initialWithdrawal);
        r.setStockMarketAllocation(stockMarketAllocation);
        r.setYearCount(yearCount);
        r.setExpensesAndMgmtFee(expensesAndMgmtFee);
        r.setWithdrawalMode(withdrawalMode);
        r.setManualAllocations(manualAllocations);
        r.setMSp500(mSp500);
        r.setMCrsp1_10(mCrsp1_10);
        r.setMCrsp6_10(mCrsp6_10);
        r.setMFfIntl(mFfIntl);
        r.setMFfEmgMkts(mFfEmgMkts);
        r.setMDjUsReit(mDjUsReit);
        r.setMOneMonth(mOneMonth);
        r.setMFiveYearUS(mFiveYearUS);
        return r;
    }
}
