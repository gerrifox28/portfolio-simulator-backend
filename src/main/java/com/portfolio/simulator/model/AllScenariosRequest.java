package com.portfolio.simulator.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/**
 * Request for the all-scenarios endpoint.
 * Accepts the two primary inputs plus the full allocation & fee settings.
 * All allocation fields default to the spreadsheet defaults.
 */
public class AllScenariosRequest {

    @Positive(message = "Starting nest egg must be positive")
    private double startingNestEgg = 1_000_000.0;

    @Positive(message = "Initial withdrawal must be positive")
    private double initialWithdrawal = 40_000.0;

    @DecimalMin(value = "0.0", message = "Expenses fee cannot be negative")
    @DecimalMax(value = "0.10", message = "Expenses fee cannot exceed 10%")
    private double expensesAndMgmtFee = 0.012;

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

    public double allocationSum() {
        return sp500 + crsp1_10 + oneMonth + fiveYearUS + crsp6_10 + ffIntl + djUsReit + ffEmgMkts;
    }
}
