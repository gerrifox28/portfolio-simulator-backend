package com.portfolio.simulator.model;

import java.util.List;

/**
 * Full API response for POST /api/simulate.
 * Contains per-year results plus top-level summary statistics.
 */
public class SimulationResponse {

    /** The inputs echo'd back so clients don't need to track them */
    private SimulationRequest inputs;

    /** Per-year simulation rows */
    private List<YearResult> yearlyResults;

    /** Number of years the portfolio survived */
    private int yearsSurvived;

    /** Whether the portfolio was exhausted before data ran out */
    private boolean portfolioExhausted;

    /** Final portfolio value (0 if exhausted) */
    private double finalPortfolioValue;

    /** The last calendar year in the simulation */
    private int finalYear;

    public static SimulationResponse of(SimulationRequest inputs, List<YearResult> results) {
        SimulationResponse resp = new SimulationResponse();
        resp.inputs = inputs;
        resp.yearlyResults = results;
        resp.yearsSurvived = results.size();

        if (!results.isEmpty()) {
            YearResult last = results.get(results.size() - 1);
            resp.finalYear = last.getYear();
            resp.finalPortfolioValue = Math.max(0, last.getPortfolioEnd());
            resp.portfolioExhausted = last.getPortfolioEnd() <= 0;
        }

        return resp;
    }

    // --- Getters & Setters ---

    public SimulationRequest getInputs() { return inputs; }
    public void setInputs(SimulationRequest inputs) { this.inputs = inputs; }

    public List<YearResult> getYearlyResults() { return yearlyResults; }
    public void setYearlyResults(List<YearResult> yearlyResults) { this.yearlyResults = yearlyResults; }

    public int getYearsSurvived() { return yearsSurvived; }
    public void setYearsSurvived(int yearsSurvived) { this.yearsSurvived = yearsSurvived; }

    public boolean isPortfolioExhausted() { return portfolioExhausted; }
    public void setPortfolioExhausted(boolean portfolioExhausted) { this.portfolioExhausted = portfolioExhausted; }

    public double getFinalPortfolioValue() { return finalPortfolioValue; }
    public void setFinalPortfolioValue(double finalPortfolioValue) { this.finalPortfolioValue = finalPortfolioValue; }

    public int getFinalYear() { return finalYear; }
    public void setFinalYear(int finalYear) { this.finalYear = finalYear; }
}
