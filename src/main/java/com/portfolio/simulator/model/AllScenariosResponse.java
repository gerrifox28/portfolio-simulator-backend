package com.portfolio.simulator.model;

import java.util.List;

/**
 * Response for POST /api/simulate/all
 * Runs all historical 40-year scenarios and returns aggregate stats.
 */
public class AllScenariosResponse {

    /** One entry per starting year (1929–1986) */
    private List<ScenarioSummary> scenarios;

    /** Total number of scenarios run */
    private int totalScenarios;

    /** Number of scenarios where portfolio was exhausted before 40 years */
    private int failureCount;

    /** Failure rate as a decimal (e.g. 0.207 = 20.7%) */
    private double failureRate;

    /** Earliest year the portfolio ran out (worst case sequence) */
    private int earliestFailureYears;

    /** Highest remaining balance after 40 years across all scenarios */
    private double highestEndingBalance;

    /** Average remaining balance after 40 years (exhausted = 0) */
    private double averageEndingBalance;

    /** The starting year that produced the highest final balance */
    private int bestStartYear;

    /** The starting year that produced the earliest failure */
    private int worstStartYear;

    // --- Getters & Setters ---

    public List<ScenarioSummary> getScenarios() { return scenarios; }
    public void setScenarios(List<ScenarioSummary> scenarios) { this.scenarios = scenarios; }

    public int getTotalScenarios() { return totalScenarios; }
    public void setTotalScenarios(int totalScenarios) { this.totalScenarios = totalScenarios; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    public double getFailureRate() { return failureRate; }
    public void setFailureRate(double failureRate) { this.failureRate = failureRate; }

    public int getEarliestFailureYears() { return earliestFailureYears; }
    public void setEarliestFailureYears(int earliestFailureYears) { this.earliestFailureYears = earliestFailureYears; }

    public double getHighestEndingBalance() { return highestEndingBalance; }
    public void setHighestEndingBalance(double highestEndingBalance) { this.highestEndingBalance = highestEndingBalance; }

    public double getAverageEndingBalance() { return averageEndingBalance; }
    public void setAverageEndingBalance(double averageFinalBalance) { this.averageEndingBalance = averageFinalBalance; }

    public int getBestStartYear() { return bestStartYear; }
    public void setBestStartYear(int bestStartYear) { this.bestStartYear = bestStartYear; }

    public int getWorstStartYear() { return worstStartYear; }
    public void setWorstStartYear(int worstStartYear) { this.worstStartYear = worstStartYear; }
}
