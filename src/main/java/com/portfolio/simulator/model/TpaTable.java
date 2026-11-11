package com.portfolio.simulator.model;

/**
 * TPA (Total Portfolio Approach) withdrawal threshold.
 *
 * The threshold for a year is the level payout rate, paid at the start of each year, that
 * would exactly exhaust the portfolio over the years remaining — assuming a 7% nominal
 * return and 3% inflation:
 *
 *   threshold = (1 − v) / (1 − v^(R+1)),   v = 1.03 / 1.07,   R = yearCount − seq
 *
 * An inflation adjustment is granted when
 *   prevWithdrawal * (1 + prevCPI) / currentBeginBalance  ≤  threshold.
 *
 * This replaced a hand-supplied lookup table (sequence years 2–45 × horizons 30–45). Every
 * cell of that table matched this formula to ~1e-13, so results are unchanged for those
 * horizons; the formula simply extends to any horizon instead of silently applying no
 * restriction outside 30–45. The same assumptions drive the PV test used by the
 * "tpa_total" withdrawal mode (see SimulatorService).
 */
public final class TpaTable {

    /** Assumed nominal portfolio return for all TPA tests. */
    public static final double ASSUMED_RETURN = 0.07;

    /** Assumed inflation for all TPA tests. */
    public static final double ASSUMED_INFLATION = 0.03;

    private TpaTable() {}

    /**
     * Returns the TPA withdrawal threshold for a given sequence year and simulation horizon.
     *
     * @param seq       sequence year (1-based); year 1 has no threshold
     * @param yearCount simulation horizon (any length)
     * @return threshold ratio; {@link Double#MAX_VALUE} (no restriction) for year 1 or for
     *         years past the horizon
     */
    public static double lookup(int seq, int yearCount) {
        if (seq < 2 || seq > yearCount) return Double.MAX_VALUE;
        int remaining = yearCount - seq;
        double v = (1.0 + ASSUMED_INFLATION) / (1.0 + ASSUMED_RETURN);
        return (1.0 - v) / (1.0 - Math.pow(v, remaining + 1));
    }
}
