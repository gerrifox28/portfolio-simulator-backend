package com.portfolio.simulator;

import com.portfolio.simulator.model.SimulationRequest;
import com.portfolio.simulator.model.YearResult;
import com.portfolio.simulator.service.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorServiceTest {

    private SimulatorService service;
    private SimulationRequest defaultRequest;

    // Tolerance in dollars for 40-year ending balance comparisons.
    // Values verified against the source Excel spreadsheet (Master Returns 03262026).
    private static final double BALANCE_TOLERANCE = 500.0;

    @BeforeEach
    void setUp() {
        service = new SimulatorService();
        defaultRequest = new SimulationRequest(); // uses spreadsheet defaults
    }

    // -------------------------------------------------------------------------
    // Basic sanity checks
    // -------------------------------------------------------------------------

    @Test
    void firstYear_portfolioBeginningEqualsStartingNestEgg() {
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(1_000_000.0, results.get(0).getPortfolioBeginning(), 0.01);
    }

    @Test
    void firstYear_withdrawalEqualsInitialWithdrawal() {
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(40_000.0, results.get(0).getAnnualWithdrawal(), 0.01);
    }

    @Test
    void secondYear_portfolioBeginningEqualsPriorYearEnd() {
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(results.get(0).getPortfolioEnd(), results.get(1).getPortfolioBeginning(), 0.01);
    }

    @Test
    void secondYear_endBalanceMatchesSpreadsheet() {
        // seq=2, year=1930, starting from 1929. Updated for actual FFIntl/REIT/EmgMkts formula.
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(560_746.0, results.get(1).getPortfolioEnd(), 5.0);
    }

    @Test
    void thirdYear_endBalanceMatchesSpreadsheet() {
        // seq=3, year=1931, starting from 1929. Updated for actual FFIntl/REIT/EmgMkts formula.
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(353_065.0, results.get(2).getPortfolioEnd(), 5.0);
    }

    @Test
    void allocationSumValidation_defaultsToOne() {
        assertEquals(1.0, defaultRequest.allocationSum(), 0.001);
    }

    @Test
    void yearRange_containsExpectedBounds() {
        assertEquals(1929, service.getMinYear());
        assertEquals(2025, service.getMaxYear());
    }

    @Test
    void sequenceNumbers_areContiguous() {
        List<YearResult> results = service.simulate(defaultRequest);
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).getSequenceNumber());
        }
    }

    @Test
    void portfolioEnd_neverExceedsReasonableBound() {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(1950);
        List<YearResult> results = service.simulate(req);
        results.forEach(r ->
            assertTrue(r.getPortfolioEnd() < 1_000_000_000,
                "Portfolio value unexpectedly huge in year " + r.getYear())
        );
    }

    @Test
    void portfolioSurvives_withLowWithdrawal() {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(1970);
        req.setInitialWithdrawal(10_000.0);
        List<YearResult> results = service.simulate(req);
        assertTrue(results.get(results.size() - 1).getPortfolioEnd() > 0,
            "Portfolio should survive with low withdrawal rate from 1970");
    }

    // -------------------------------------------------------------------------
    // All-scenarios: ending balance after 40 years
    // Source: "claude analysis.png" — spreadsheet column "End 40 yr bal"
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Starting {0}: expected 40-yr ending balance ~${1}")
    @MethodSource("survivedScenarios")
    void scenario_survivesFullyAndEndBalanceMatchesSpreadsheet(int startYear, double expectedEndBalance) {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(startYear);

        List<YearResult> results = service.simulate(req);

        assertTrue(results.size() >= 40,
            "Portfolio starting in " + startYear + " should survive at least 40 years, but only lasted " + results.size());
        assertEquals(expectedEndBalance, results.get(39).getPortfolioEnd(), BALANCE_TOLERANCE,
            "Ending balance mismatch for start year " + startYear);
    }

    static Stream<Arguments> survivedScenarios() {
        return Stream.of(
            Arguments.of(1930,  559_874.0),
            Arguments.of(1931,  3_469_716.0),
            Arguments.of(1932,  11_971_175.0),
            Arguments.of(1933,  14_066_435.0),
            Arguments.of(1934,  5_041_337.0),
            Arguments.of(1935,  4_083_950.0),
            Arguments.of(1936,  1_480_708.0),
            Arguments.of(1938,  4_962_727.0),
            Arguments.of(1939,  2_040_361.0),
            Arguments.of(1940,  2_625_781.0),
            Arguments.of(1941,  5_530_917.0),
            Arguments.of(1942,  10_802_765.0),
            Arguments.of(1943,  11_955_702.0),
            Arguments.of(1944,  9_482_131.0),
            Arguments.of(1945,  7_318_616.0),
            Arguments.of(1946,  3_157_649.0),
            Arguments.of(1947,  11_404_187.0),
            Arguments.of(1948,  14_628_948.0),
            Arguments.of(1949,  18_770_799.0),
            Arguments.of(1950,  17_139_612.0),
            Arguments.of(1951,  11_770_772.0),
            Arguments.of(1952,  12_543_555.0),
            Arguments.of(1953,  11_641_966.0),
            Arguments.of(1954,  15_414_246.0),
            Arguments.of(1955,  5_918_095.0),
            Arguments.of(1956,  3_283_444.0),
            Arguments.of(1957,  4_237_502.0),
            Arguments.of(1958,  9_651_760.0),
            Arguments.of(1959,  2_046_207.0),
            Arguments.of(1960,  1_412_480.0),
            Arguments.of(1961,  2_167_551.0),
            Arguments.of(1963,  1_527_011.0),
            Arguments.of(1971,  1_882_888.0),
            Arguments.of(1972,  213_195.0),
            Arguments.of(1974,  7_826_218.0),
            Arguments.of(1975,  21_912_749.0),
            Arguments.of(1976,  13_937_552.0),
            Arguments.of(1977,  11_167_013.0),
            Arguments.of(1978,  14_922_002.0),
            Arguments.of(1979,  13_633_199.0),
            Arguments.of(1980,  13_884_507.0),
            Arguments.of(1981,  12_218_727.0),
            Arguments.of(1982,  15_637_224.0),
            Arguments.of(1983,  10_589_195.0),
            Arguments.of(1984,  9_451_492.0),
            Arguments.of(1985,  10_060_809.0),
            Arguments.of(1986,  7_452_415.0)
        );
    }

    // -------------------------------------------------------------------------
    // All-scenarios: portfolio exhausted before (or at) 40 years
    // Source: "claude analysis.png" — spreadsheet column "Expired yr"
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Starting {0}: expected to exhaust in year {1}")
    @MethodSource("exhaustedScenarios")
    void scenario_exhaustsAtExpectedYear(int startYear, int expectedYearsSurvived) {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(startYear);

        List<YearResult> results = service.simulate(req);

        assertEquals(expectedYearsSurvived, results.size(),
            "Portfolio starting in " + startYear + " should exhaust in year " + expectedYearsSurvived);
        assertTrue(results.get(results.size() - 1).getPortfolioEnd() <= 0,
            "Final portfolio end should be zero or negative for start year " + startYear);
    }

    static Stream<Arguments> exhaustedScenarios() {
        return Stream.of(
            Arguments.of(1929, 23),
            Arguments.of(1937, 36),
            Arguments.of(1962, 34),
            Arguments.of(1964, 38),
            Arguments.of(1965, 31),
            Arguments.of(1966, 28),
            Arguments.of(1967, 38),
            Arguments.of(1968, 26),
            Arguments.of(1969, 24),
            Arguments.of(1970, 40),
            Arguments.of(1973, 37)
        );
    }
}
