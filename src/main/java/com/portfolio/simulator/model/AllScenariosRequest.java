package com.portfolio.simulator.model;

import jakarta.validation.constraints.Positive;

/**
 * Simplified request for the all-scenarios endpoint.
 * Only exposes the 2 user-facing inputs — allocation uses spreadsheet defaults.
 */
public class AllScenariosRequest {

    @Positive(message = "Starting nest egg must be positive")
    private double startingNestEgg = 1_000_000.0;

    @Positive(message = "Initial withdrawal must be positive")
    private double initialWithdrawal = 40_000.0;

    public double getStartingNestEgg() { return startingNestEgg; }
    public void setStartingNestEgg(double startingNestEgg) { this.startingNestEgg = startingNestEgg; }

    public double getInitialWithdrawal() { return initialWithdrawal; }
    public void setInitialWithdrawal(double initialWithdrawal) { this.initialWithdrawal = initialWithdrawal; }
}
