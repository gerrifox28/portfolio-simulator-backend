package com.portfolio.simulator;

import com.portfolio.simulator.model.SimulationRequest;
import com.portfolio.simulator.model.YearResult;
import com.portfolio.simulator.service.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorServiceTest {

    private SimulatorService service;
    private SimulationRequest defaultRequest;

    @BeforeEach
    void setUp() {
        service = new SimulatorService();
        defaultRequest = new SimulationRequest(); // uses spreadsheet defaults
    }

    // --- Values verified against the source Excel spreadsheet ---

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
        // Spreadsheet row 7 (seq=2, year=1930): I7 ≈ 564,750
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(564_750.0, results.get(1).getPortfolioEnd(), 5.0);
    }

    @Test
    void thirdYear_endBalanceMatchesSpreadsheet() {
        // Spreadsheet row 8 (seq=3, year=1931): I8 ≈ 355,471
        List<YearResult> results = service.simulate(defaultRequest);
        assertEquals(355_471.0, results.get(2).getPortfolioEnd(), 5.0);
    }

    @Test
    void portfolioExhausts_whenMarketIsConsistentlyPoor() {
        // Starting in 1929 with default allocation — portfolio should exhaust
        List<YearResult> results = service.simulate(defaultRequest);
        YearResult last = results.get(results.size() - 1);
        assertTrue(last.getPortfolioEnd() <= 0,
            "Expected portfolio to be exhausted starting from 1929 crash");
    }

    @Test
    void portfolioSurvives_withLowWithdrawal() {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(1970);
        req.setInitialWithdrawal(10_000.0); // Very conservative withdrawal
        List<YearResult> results = service.simulate(req);
        YearResult last = results.get(results.size() - 1);
        assertTrue(last.getPortfolioEnd() > 0,
            "Portfolio should survive with low withdrawal rate from 1970");
    }

    @Test
    void allocationSumValidation_defaultsToOne() {
        assertEquals(1.0, defaultRequest.allocationSum(), 0.001);
    }

    @Test
    void yearRange_containsExpectedBounds() {
        assertEquals(1926, service.getMinYear());
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
}
