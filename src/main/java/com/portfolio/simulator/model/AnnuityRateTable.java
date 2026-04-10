package com.portfolio.simulator.model;

import java.util.Map;

/**
 * Lookup table for "Increasing Income" annuity payout rates by age.
 * Source: Annuity Rates.xlsm (ages 49–80).
 *
 * Rates are annual payout as a fraction of the purchase amount (e.g. 0.069 = 6.9%).
 * "Increasing Income" means the annual payout grows with CPI each year, capped at 3%.
 */
public final class AnnuityRateTable {

    private AnnuityRateTable() {}

    private static final Map<Integer, double[]> RATES = Map.ofEntries(
        // age → { singleRate, jointRate }
        Map.entry(49, new double[]{0.0520, 0.0470}),
        Map.entry(50, new double[]{0.0520, 0.0470}),
        Map.entry(51, new double[]{0.0530, 0.0480}),
        Map.entry(52, new double[]{0.0540, 0.0490}),
        Map.entry(53, new double[]{0.0550, 0.0500}),
        Map.entry(54, new double[]{0.0560, 0.0510}),
        Map.entry(55, new double[]{0.0570, 0.0520}),
        Map.entry(56, new double[]{0.0580, 0.0530}),
        Map.entry(57, new double[]{0.0590, 0.0540}),
        Map.entry(58, new double[]{0.0600, 0.0550}),
        Map.entry(59, new double[]{0.0610, 0.0560}),
        Map.entry(60, new double[]{0.0620, 0.0570}),
        Map.entry(61, new double[]{0.0635, 0.0585}),
        Map.entry(62, new double[]{0.0650, 0.0600}),
        Map.entry(63, new double[]{0.0665, 0.0615}),
        Map.entry(64, new double[]{0.0680, 0.0630}),
        Map.entry(65, new double[]{0.0690, 0.0640}),
        Map.entry(66, new double[]{0.0700, 0.0650}),
        Map.entry(67, new double[]{0.0710, 0.0660}),
        Map.entry(68, new double[]{0.0720, 0.0670}),
        Map.entry(69, new double[]{0.0730, 0.0680}),
        Map.entry(70, new double[]{0.0740, 0.0690}),
        Map.entry(71, new double[]{0.0750, 0.0700}),
        Map.entry(72, new double[]{0.0760, 0.0710}),
        Map.entry(73, new double[]{0.0770, 0.0720}),
        Map.entry(74, new double[]{0.0780, 0.0730}),
        Map.entry(75, new double[]{0.0790, 0.0740}),
        Map.entry(76, new double[]{0.0800, 0.0750}),
        Map.entry(77, new double[]{0.0810, 0.0760}),
        Map.entry(78, new double[]{0.0820, 0.0770}),
        Map.entry(79, new double[]{0.0830, 0.0780}),
        Map.entry(80, new double[]{0.0840, 0.0790})
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
}
