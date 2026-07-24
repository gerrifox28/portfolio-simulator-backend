package com.portfolio.simulator.model;

import java.util.Map;

/**
 * Lookup table for "Increasing Income" annuity payout rates by age.
 * Source: Allianz Annuity Percentages.xlsx, May 2026 (ages 49–80).
 *
 * Rates are annual payout as a fraction of the purchase amount (e.g. 0.069 = 6.9%).
 * "Increasing Income" means the annual payout grows with CPI each year, capped at 3%.
 */
public final class AnnuityRateTable {

    private AnnuityRateTable() {}

    private static final Map<Integer, double[]> RATES = Map.ofEntries(
        // age → { singleRate, jointRate }  source: Allianz Annuity Percentages.xlsx (May 2026)
        Map.entry(49, new double[]{0.0530, 0.0480}),
        Map.entry(50, new double[]{0.0530, 0.0480}),
        Map.entry(51, new double[]{0.0540, 0.0490}),
        Map.entry(52, new double[]{0.0550, 0.0500}),
        Map.entry(53, new double[]{0.0560, 0.0510}),
        Map.entry(54, new double[]{0.0570, 0.0520}),
        Map.entry(55, new double[]{0.0580, 0.0530}),
        Map.entry(56, new double[]{0.0590, 0.0540}),
        Map.entry(57, new double[]{0.0605, 0.0555}),
        Map.entry(58, new double[]{0.0615, 0.0565}),
        Map.entry(59, new double[]{0.0625, 0.0575}),
        Map.entry(60, new double[]{0.0640, 0.0590}),
        Map.entry(61, new double[]{0.0655, 0.0605}),
        Map.entry(62, new double[]{0.0670, 0.0620}),
        Map.entry(63, new double[]{0.0685, 0.0635}),
        Map.entry(64, new double[]{0.0695, 0.0645}),
        Map.entry(65, new double[]{0.0715, 0.0665}),
        Map.entry(66, new double[]{0.0725, 0.0675}),
        Map.entry(67, new double[]{0.0735, 0.0685}),
        Map.entry(68, new double[]{0.0755, 0.0705}),
        Map.entry(69, new double[]{0.0765, 0.0715}),
        Map.entry(70, new double[]{0.0780, 0.0730}),
        Map.entry(71, new double[]{0.0785, 0.0735}),
        Map.entry(72, new double[]{0.0790, 0.0740}),
        Map.entry(73, new double[]{0.0795, 0.0745}),
        Map.entry(74, new double[]{0.0800, 0.0750}),
        Map.entry(75, new double[]{0.0805, 0.0755}),
        Map.entry(76, new double[]{0.0810, 0.0760}),
        Map.entry(77, new double[]{0.0820, 0.0770}),
        Map.entry(78, new double[]{0.0830, 0.0780}),
        Map.entry(79, new double[]{0.0840, 0.0790}),
        Map.entry(80, new double[]{0.0850, 0.0800})
    );

    /**
     * Per-age deferral bonus rate: added to the base payout rate once for each full year
     * between annuity purchase (simulation year 1) and the income start year, to reward
     * deferring income. Source: Annuity Increase Percentages.xlsx.
     */
    private static final Map<Integer, Double> ANNUAL_INCREASE = Map.ofEntries(
        Map.entry(49, 0.0045),
        Map.entry(50, 0.0045),
        Map.entry(51, 0.0046),
        Map.entry(52, 0.0047),
        Map.entry(53, 0.0048),
        Map.entry(54, 0.0049),
        Map.entry(55, 0.0050),
        Map.entry(56, 0.0051),
        Map.entry(57, 0.0052),
        Map.entry(58, 0.0053),
        Map.entry(59, 0.0054),
        Map.entry(60, 0.0055),
        Map.entry(61, 0.0056),
        Map.entry(62, 0.0057),
        Map.entry(63, 0.0058),
        Map.entry(64, 0.0059),
        Map.entry(65, 0.0060),
        Map.entry(66, 0.0061),
        Map.entry(67, 0.0062),
        Map.entry(68, 0.0063),
        Map.entry(69, 0.0064),
        Map.entry(70, 0.0065),
        Map.entry(71, 0.0066),
        Map.entry(72, 0.0067),
        Map.entry(73, 0.0068),
        Map.entry(74, 0.0069),
        Map.entry(75, 0.0070),
        Map.entry(76, 0.0071),
        Map.entry(77, 0.0072),
        Map.entry(78, 0.0073),
        Map.entry(79, 0.0074),
        Map.entry(80, 0.0075)
    );

    public static final int MIN_AGE = 49;
    public static final int MAX_AGE = 80;

    /**
     * Returns the annual payout rate for the given age and coverage type.
     *
     * @param age     purchaser's age at annuity start (49–80)
     * @param isJoint true for joint-life coverage, false for single-life
     * @return annual payout rate as a decimal (e.g. 0.069)
     * @throws IllegalArgumentException if age is outside the supported range
     */
    public static double lookup(int age, boolean isJoint) {
        double[] row = RATES.get(age);
        if (row == null) {
            throw new IllegalArgumentException(
                "Age " + age + " is outside the supported annuity range (" + MIN_AGE + "–" + MAX_AGE + ")");
        }
        return isJoint ? row[1] : row[0];
    }

    /**
     * Returns the per-year deferral bonus rate for the given purchase age.
     *
     * @param age purchaser's age at annuity purchase (49–80)
     * @return annual increase rate as a decimal (e.g. 0.006 = 0.60%)
     * @throws IllegalArgumentException if age is outside the supported range
     */
    public static double lookupAnnualIncreasePct(int age) {
        Double pct = ANNUAL_INCREASE.get(age);
        if (pct == null) {
            throw new IllegalArgumentException(
                "Age " + age + " is outside the supported annuity range (" + MIN_AGE + "–" + MAX_AGE + ")");
        }
        return pct;
    }
}
