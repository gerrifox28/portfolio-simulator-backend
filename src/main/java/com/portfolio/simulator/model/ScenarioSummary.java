package com.portfolio.simulator.model;

/**
 * Summary of a single 40-year scenario starting from a given year.
 * Used to populate the outcomes bar chart.
 */
public class ScenarioSummary {

    /** The calendar year this scenario started */
    private int startYear;

    /** Final portfolio balance after 40 years (0 if exhausted) */
    private double endingBalance;

    /** Whether the portfolio ran out before 40 years */
    private boolean failed;

    /** How many years the portfolio survived (max 40) */
    private int yearsSurvived;

    public int getStartYear() { return startYear; }
    public void setStartYear(int startYear) { this.startYear = startYear; }

    public double getEndingBalance() { return endingBalance; }
    public void setEndingBalance(double endingBalance) { this.endingBalance = endingBalance; }

    public boolean isFailed() { return failed; }
    public void setFailed(boolean failed) { this.failed = failed; }

    public int getYearsSurvived() { return yearsSurvived; }
    public void setYearsSurvived(int yearsSurvived) { this.yearsSurvived = yearsSurvived; }
}
