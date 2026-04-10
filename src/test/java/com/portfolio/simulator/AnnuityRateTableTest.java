package com.portfolio.simulator;

import com.portfolio.simulator.model.AnnuityRateTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AnnuityRateTableTest {

    // ── Known spot-checks from Annuity Rates.xlsm ────────────────────────────

    @ParameterizedTest(name = "Age {0} single = {1}")
    @CsvSource({
        "49, 0.0520",
        "58, 0.0600",
        "65, 0.0690",
        "72, 0.0760",
        "80, 0.0840",
    })
    void singleRate_matchesSpreadsheet(int age, double expected) {
        assertEquals(expected, AnnuityRateTable.lookup(age, false), 0.0001,
            "Single-life rate for age " + age + " should match spreadsheet");
    }

    @ParameterizedTest(name = "Age {0} joint = {1}")
    @CsvSource({
        "49, 0.0470",
        "58, 0.0550",
        "65, 0.0640",
        "72, 0.0710",
        "80, 0.0790",
    })
    void jointRate_matchesSpreadsheet(int age, double expected) {
        assertEquals(expected, AnnuityRateTable.lookup(age, true), 0.0001,
            "Joint-life rate for age " + age + " should match spreadsheet");
    }

    // ── Structural properties ─────────────────────────────────────────────────

    @Test
    void singleRate_alwaysHigherThanJoint_forSameAge() {
        for (int age = AnnuityRateTable.MIN_AGE; age <= AnnuityRateTable.MAX_AGE; age++) {
            double single = AnnuityRateTable.lookup(age, false);
            double joint  = AnnuityRateTable.lookup(age, true);
            assertTrue(single > joint,
                "Single-life rate should exceed joint rate at age " + age);
        }
    }

    @Test
    void rates_increaseMonotonicallyWithAge() {
        for (int age = AnnuityRateTable.MIN_AGE + 1; age <= AnnuityRateTable.MAX_AGE; age++) {
            double prev   = AnnuityRateTable.lookup(age - 1, false);
            double current = AnnuityRateTable.lookup(age, false);
            assertTrue(current >= prev,
                "Single rate at age " + age + " should be >= rate at age " + (age - 1));
        }
    }

    @Test
    void allAgesInRange_returnValidRate() {
        for (int age = AnnuityRateTable.MIN_AGE; age <= AnnuityRateTable.MAX_AGE; age++) {
            double rate = AnnuityRateTable.lookup(age, false);
            assertTrue(rate > 0.0 && rate < 1.0,
                "Rate at age " + age + " should be a valid decimal fraction");
        }
    }

    // ── Boundary / error cases ────────────────────────────────────────────────

    @Test
    void ageBelowMinimum_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> AnnuityRateTable.lookup(AnnuityRateTable.MIN_AGE - 1, false),
            "Age below 49 should throw");
    }

    @Test
    void ageAboveMaximum_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> AnnuityRateTable.lookup(AnnuityRateTable.MAX_AGE + 1, false),
            "Age above 80 should throw");
    }

    @Test
    void boundaryAges_doNotThrow() {
        assertDoesNotThrow(() -> AnnuityRateTable.lookup(AnnuityRateTable.MIN_AGE, false));
        assertDoesNotThrow(() -> AnnuityRateTable.lookup(AnnuityRateTable.MAX_AGE, false));
        assertDoesNotThrow(() -> AnnuityRateTable.lookup(AnnuityRateTable.MIN_AGE, true));
        assertDoesNotThrow(() -> AnnuityRateTable.lookup(AnnuityRateTable.MAX_AGE, true));
    }
}
