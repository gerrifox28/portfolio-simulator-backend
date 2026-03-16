package com.portfolio.simulator.model;

import jakarta.validation.constraints.*;

/**
 * Request body for POST /api/simulate.
 * Maps directly to the "Inputs and Chart" sheet.
 */
public class SimulationRequest {

    @Min(value = 1926, message = "Start year must be 1926 or later (earliest available data)")
    @Max(value = 2025, message = "Start year must be 2025 or earlier")
    private int startYear = 1929;

    @Positive(message = "Starting nest egg must be positive")
    private double startingNestEgg = 1_000_000.0;

    @Positive(message = "Initial withdrawal must be positive")
    private double initialWithdrawal = 40_000.0;

    @DecimalMin(value = "0.0", message = "Expenses fee cannot be negative")
    @DecimalMax(value = "0.10", message = "Expenses fee cannot exceed 10%")
    private double expensesAndMgmtFee = 0.012;

    // --- Asset allocation weights ---
    // Each must be between 0 and 1. The controller validates they sum to 1.

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double sp500 = 0.0;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double crsp1_10 = 0.31110;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double oneMonth = 0.05;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double fiveYearUS = 0.25;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double crsp6_10 = 0.0549;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double ffIntl = 0.162;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double djUsReit = 0.10;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private double ffEmgMkts = 0.072;

    // --- Getters & Setters ---

    public int getStartYear() { return startYear; }
    public void setStartYear(int startYear) { this.startYear = startYear; }

    public double getStartingNestEgg() { return startingNestEgg; }
    public void setStartingNestEgg(double startingNestEgg) { this.startingNestEgg = startingNestEgg; }

    public double getInitialWithdrawal() { return initialWithdrawal; }
    public void setInitialWithdrawal(double initialWithdrawal) { this.initialWithdrawal = initialWithdrawal; }

    public double getExpensesAndMgmtFee() { return expensesAndMgmtFee; }
    public void setExpensesAndMgmtFee(double expensesAndMgmtFee) { this.expensesAndMgmtFee = expensesAndMgmtFee; }

    public double getSp500() { return sp500; }
    public void setSp500(double sp500) { this.sp500 = sp500; }

    public double getCrsp1_10() { return crsp1_10; }
    public void setCrsp1_10(double crsp1_10) { this.crsp1_10 = crsp1_10; }

    public double getOneMonth() { return oneMonth; }
    public void setOneMonth(double oneMonth) { this.oneMonth = oneMonth; }

    public double getFiveYearUS() { return fiveYearUS; }
    public void setFiveYearUS(double fiveYearUS) { this.fiveYearUS = fiveYearUS; }

    public double getCrsp6_10() { return crsp6_10; }
    public void setCrsp6_10(double crsp6_10) { this.crsp6_10 = crsp6_10; }

    public double getFfIntl() { return ffIntl; }
    public void setFfIntl(double ffIntl) { this.ffIntl = ffIntl; }

    public double getDjUsReit() { return djUsReit; }
    public void setDjUsReit(double djUsReit) { this.djUsReit = djUsReit; }

    public double getFfEmgMkts() { return ffEmgMkts; }
    public void setFfEmgMkts(double ffEmgMkts) { this.ffEmgMkts = ffEmgMkts; }

    /** Returns the sum of all allocation weights. Should equal 1.0. */
    public double allocationSum() {
        return sp500 + crsp1_10 + oneMonth + fiveYearUS + crsp6_10
             + ffIntl + djUsReit + ffEmgMkts;
    }
}
