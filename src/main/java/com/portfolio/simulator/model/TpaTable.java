package com.portfolio.simulator.model;

/**
 * TPA (Total Portfolio Approach) withdrawal threshold.
 *
 * The threshold for a year is the level payout rate, paid at the start of each year and
 * growing with inflation, that would exactly exhaust the portfolio over the years
 * remaining at an assumed nominal return r and inflation i:
 *
 *   threshold = (1 − v) / (1 − v^(R+1)),   v = (1 + i) / (1 + r),   R = yearCount − seq
 *
 * (when r = i, v = 1 and the threshold is simply 1 / (R+1)). An inflation adjustment is
 * granted when
 *   prevWithdrawal * (1 + prevCPI) / currentBeginBalance  ≤  threshold.
 *
 * The defaults (7% return, 3% inflation) reproduce the hand-supplied lookup table this
 * replaced (sequence years 2–45 × horizons 30–45): every cell matched to ~1e-13. Users can
 * override r and i per request (SimulationRequest.tpaAssumedReturn / tpaAssumedInflation);
 * the same assumptions drive the PV test of the "tpa_total" withdrawal mode.
 */
public final class TpaTable {

    /** Default assumed nominal portfolio return for TPA tests. */
    public static final double DEFAULT_RETURN = 0.07;

    /** Default assumed inflation for TPA tests. */
    public static final double DEFAULT_INFLATION = 0.03;

    private TpaTable() {}

    /** Threshold at the default 7% return / 3% inflation assumptions. */
    public static double lookup(int seq, int yearCount) {
        return lookup(seq, yearCount, DEFAULT_RETURN, DEFAULT_INFLATION);
    }

    /**
     * Returns the TPA withdrawal threshold for a given sequence year and simulation horizon.
     *
     * @param seq             sequence year (1-based); year 1 has no threshold
     * @param yearCount       simulation horizon (any length)
     * @param assumedReturn   assumed nominal return, e.g. 0.07
     * @param assumedInflation assumed inflation, e.g. 0.03
     * @return threshold ratio; {@link Double#MAX_VALUE} (no restriction) for year 1 or for
     *         years past the horizon
     */
    public static double lookup(int seq, int yearCount, double assumedReturn, double assumedInflation) {
        if (seq < 2 || seq > yearCount) return Double.MAX_VALUE;
        int remaining = yearCount - seq;
        double v = (1.0 + assumedInflation) / (1.0 + assumedReturn);
        if (Math.abs(v - 1.0) < 1e-12) return 1.0 / (remaining + 1);
        return (1.0 - v) / (1.0 - Math.pow(v, remaining + 1));
    }
}
