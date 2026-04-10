package com.portfolio.simulator.model;

/**
 * Response for POST /api/simulate/compare.
 * Contains parallel all-scenarios results for the portfolio-only and annuity+portfolio runs.
 */
public class AnnuityCompareResponse {

    /** Results when the full nest egg is invested — no annuity. */
    private AllScenariosResponse withoutAnnuity;

    /** Results when annuityPercentage of the nest egg is annuitized and the rest is invested. */
    private AllScenariosResponse withAnnuity;

    /** The annuity payout rate used (e.g. 0.069 = 6.9%). */
    private double annuityRate;

    /** Annual income from the annuity in year 1 (annuityPercentage × nestEgg × annuityRate). */
    private double initialAnnuityIncome;

    public AllScenariosResponse getWithoutAnnuity() { return withoutAnnuity; }
    public void setWithoutAnnuity(AllScenariosResponse v) { this.withoutAnnuity = v; }

    public AllScenariosResponse getWithAnnuity() { return withAnnuity; }
    public void setWithAnnuity(AllScenariosResponse v) { this.withAnnuity = v; }

    public double getAnnuityRate() { return annuityRate; }
    public void setAnnuityRate(double v) { this.annuityRate = v; }

    public double getInitialAnnuityIncome() { return initialAnnuityIncome; }
    public void setInitialAnnuityIncome(double v) { this.initialAnnuityIncome = v; }
}
