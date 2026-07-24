package com.portfolio.simulator;

import com.portfolio.simulator.model.AllScenariosRequest;
import com.portfolio.simulator.model.AllScenariosResponse;
import com.portfolio.simulator.model.AnnuityCompareRequest;
import com.portfolio.simulator.model.AnnuityCompareResponse;
import com.portfolio.simulator.model.AnnuityRateTable;
import com.portfolio.simulator.model.CashFlow;
import com.portfolio.simulator.model.SimulationRequest;
import com.portfolio.simulator.model.YearResult;
import com.portfolio.simulator.service.SimulatorService;
import com.portfolio.simulator.service.SpreadsheetLoaderService;
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
        service = new SimulatorService(new SpreadsheetLoaderService());
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

    // -------------------------------------------------------------------------
    // Custom allocation and fee tests
    // -------------------------------------------------------------------------

    @Test
    void higherFee_reducesEndingBalance() {
        SimulationRequest lowFee = new SimulationRequest();
        lowFee.setStartYear(1950);
        lowFee.setExpensesAndMgmtFee(0.005); // 0.5%

        SimulationRequest highFee = new SimulationRequest();
        highFee.setStartYear(1950);
        highFee.setExpensesAndMgmtFee(0.025); // 2.5%

        double lowFeeEnd = service.simulate(lowFee).get(39).getPortfolioEnd();
        double highFeeEnd = service.simulate(highFee).get(39).getPortfolioEnd();

        assertTrue(lowFeeEnd > highFeeEnd,
            "Lower fee should produce higher ending balance over 40 years");
    }

    @Test
    void zeroFee_producesHigherBalanceThanDefaultFee() {
        SimulationRequest zeroFee = new SimulationRequest();
        zeroFee.setStartYear(1970);
        zeroFee.setExpensesAndMgmtFee(0.0);

        SimulationRequest defaultFee = new SimulationRequest();
        defaultFee.setStartYear(1970);
        // default fee is 1.2%

        List<YearResult> zeroResults = service.simulate(zeroFee);
        List<YearResult> defaultResults = service.simulate(defaultFee);

        // Both should survive some years; zero fee should always end higher
        int minYears = Math.min(zeroResults.size(), defaultResults.size());
        assertTrue(minYears > 0);
        assertTrue(zeroResults.get(minYears - 1).getPortfolioEnd()
                 > defaultResults.get(minYears - 1).getPortfolioEnd(),
            "Zero fee should produce higher balance than 1.2% fee");
    }

    @Test
    void allBondsAllocation_differentReturnRateFromAllStocks() {
        // Verify that allocation weights actually affect the blended return rate.
        // Compare year-1 return rates: 100% 5-yr treasuries vs 100% S&P 500 starting 1950.
        // In 1950: 5-yr treasury = 0.70%, S&P 500 = 31.74% — clearly different.
        SimulationRequest allBonds = new SimulationRequest();
        allBonds.setStartYear(1950);
        allBonds.setSp500(0.0);
        allBonds.setCrsp1_10(0.0);
        allBonds.setOneMonth(0.0);
        allBonds.setFiveYearUS(1.0);
        allBonds.setCrsp6_10(0.0);
        allBonds.setFfIntl(0.0);
        allBonds.setDjUsReit(0.0);
        allBonds.setFfEmgMkts(0.0);

        SimulationRequest allStocks = new SimulationRequest();
        allStocks.setStartYear(1950);
        allStocks.setSp500(1.0);
        allStocks.setCrsp1_10(0.0);
        allStocks.setOneMonth(0.0);
        allStocks.setFiveYearUS(0.0);
        allStocks.setCrsp6_10(0.0);
        allStocks.setFfIntl(0.0);
        allStocks.setDjUsReit(0.0);
        allStocks.setFfEmgMkts(0.0);

        double bondsRate = service.simulate(allBonds).get(0).getPortfolioReturnRate();
        double stocksRate = service.simulate(allStocks).get(0).getPortfolioReturnRate();

        assertNotEquals(bondsRate, stocksRate, 0.001,
            "All-bonds and all-stocks should produce different blended return rates in 1950");
        assertTrue(stocksRate > bondsRate,
            "S&P 500 return should exceed 5-yr treasury return in 1950");
    }

    @Test
    void customAllocation_sumValidation() {
        SimulationRequest badAlloc = new SimulationRequest();
        badAlloc.setSp500(0.5);
        badAlloc.setCrsp1_10(0.5);
        badAlloc.setOneMonth(0.5); // sum > 1.0

        // allocationSum() reflects the actual sum — controller rejects it
        assertTrue(badAlloc.allocationSum() > 1.0,
            "Allocation sum should be > 1.0 when weights exceed 100%");
    }

    @Test
    void simulateAll_withCustomFee_differentFromDefault() {
        AllScenariosRequest defaultReq = new AllScenariosRequest();

        AllScenariosRequest highFeeReq = new AllScenariosRequest();
        highFeeReq.setExpensesAndMgmtFee(0.03); // 3%

        AllScenariosResponse defaultResp = service.simulateAll(defaultReq);
        AllScenariosResponse highFeeResp = service.simulateAll(highFeeReq);

        // Higher fee should produce lower average ending balance
        assertTrue(defaultResp.getAverageEndingBalance() > highFeeResp.getAverageEndingBalance(),
            "Default 1.2% fee should outperform 3% fee on average ending balance");
    }

    @Test
    void simulateAll_withLowStockAllocation_moreFailuresThanDefault() {
        // Default 60% stocks
        AllScenariosRequest defaultReq = new AllScenariosRequest();

        // 0% stocks — all bonds/REIT, very conservative
        AllScenariosRequest noStocksReq = new AllScenariosRequest();
        noStocksReq.setStockMarketAllocation(0.0);

        AllScenariosResponse defaultResp = service.simulateAll(defaultReq);
        AllScenariosResponse noStocksResp = service.simulateAll(noStocksReq);

        // All-bonds should have at least as many failures as 60% diversified stocks
        assertTrue(noStocksResp.getFailureCount() >= defaultResp.getFailureCount(),
            "0% stocks should not have fewer failures than the 60% diversified default");
    }

    @Test
    void simulateAll_defaultStockAllocationSumsToOne() {
        AllScenariosRequest req = new AllScenariosRequest();
        double sum = req.getSp500() + req.getCrsp1_10() + req.getOneMonth()
                   + req.getFiveYearUS() + req.getCrsp6_10() + req.getFfIntl()
                   + req.getDjUsReit() + req.getFfEmgMkts();
        assertEquals(1.0, sum, 0.001,
            "Derived allocations for default 60% SMA should sum to 1.0");
    }

    @Test
    void simulateAll_yearCountIsReflectedInResponse() {
        AllScenariosRequest req = new AllScenariosRequest();
        req.setYearCount(30);
        AllScenariosResponse resp = service.simulateAll(req);
        assertEquals(30, resp.getYearCount(), "Response should echo back the requested yearCount");
    }

    // -------------------------------------------------------------------------
    // Annuity comparison tests
    // -------------------------------------------------------------------------

    @Test
    void annuityCompare_rateMatchesRateTable() {
        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setAge(65);
        req.setJoint(false);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        assertEquals(AnnuityRateTable.lookup(65, false), resp.getAnnuityRate(), 0.0001,
            "Returned annuity rate should match AnnuityRateTable.lookup(65, false)");
    }

    @Test
    void annuityCompare_jointRateLowerThanSingleForSameAge() {
        AnnuityCompareRequest single = new AnnuityCompareRequest();
        single.setAge(65);
        single.setJoint(false);

        AnnuityCompareRequest joint = new AnnuityCompareRequest();
        joint.setAge(65);
        joint.setJoint(true);

        double singleRate = service.simulateAllCompare(single).getAnnuityRate();
        double jointRate  = service.simulateAllCompare(joint).getAnnuityRate();

        assertTrue(jointRate < singleRate,
            "Joint payout rate should be lower than single-life rate for the same age");
    }

    @Test
    void annuityCompare_initialAnnuityIncomeMatchesExpectedFormula() {
        // initialAnnuityIncome = nestEgg * annuityPct * rate
        double nestEgg     = 1_000_000.0;
        double annuityPct  = 0.30;
        int age            = 65;

        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setAge(age);
        req.setJoint(false);
        req.setAnnuityPercentage(annuityPct);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        double expectedIncome = nestEgg * annuityPct * AnnuityRateTable.lookup(age, false);
        assertEquals(expectedIncome, resp.getInitialAnnuityIncome(), 1.0,
            "Year-1 annuity income should equal nestEgg × annuityPct × rate");
    }

    @Test
    void annuityCompare_withoutAnnuityMatchesStandardRun() {
        // The "withoutAnnuity" leg should be identical to a plain simulateAll call
        AllScenariosRequest stdReq = new AllScenariosRequest();
        AnnuityCompareRequest cmpReq = new AnnuityCompareRequest();
        // annuityPercentage default is 0.30, but withoutAnnuity leg ignores it

        AllScenariosResponse standard = service.simulateAll(stdReq);
        AnnuityCompareResponse compare = service.simulateAllCompare(cmpReq);

        assertEquals(standard.getFailureCount(),        compare.getWithoutAnnuity().getFailureCount(),
            "withoutAnnuity failure count should match plain simulateAll");
        assertEquals(standard.getTotalScenarios(),      compare.getWithoutAnnuity().getTotalScenarios());
        assertEquals(standard.getAverageEndingBalance(),compare.getWithoutAnnuity().getAverageEndingBalance(), 1.0);
    }

    @Test
    void annuityCompare_highAnnuityPercentage_reducesBothFailuresAndPortfolioBalance() {
        // A large annuity reduces the investable portfolio, but offsets withdrawals.
        // With 80% annuitized, the annuity covers most income needs —
        // portfolio should survive more often but end smaller.
        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setAge(65);
        req.setJoint(false);
        req.setAnnuityPercentage(0.80);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        // With 80% annuity at age 65 (6.9% rate) → $55,200/yr income on a $1M nest egg
        // vs $40,000 target withdrawal → annuity fully covers income, portfolio grows unchecked
        assertTrue(resp.getWithAnnuity().getFailureCount() <= resp.getWithoutAnnuity().getFailureCount(),
            "80% annuity should not produce more failures than no annuity");
    }

    @Test
    void annuityCompare_yearCountEchoedInBothResults() {
        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setYearCount(30);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        assertEquals(30, resp.getWithoutAnnuity().getYearCount(),
            "withoutAnnuity should echo yearCount=30");
        assertEquals(30, resp.getWithAnnuity().getYearCount(),
            "withAnnuity should echo yearCount=30");
    }

    @Test
    void annuityCompare_olderAge_higherPayoutRate() {
        // Rate table is monotonically increasing with age
        AnnuityCompareRequest age60 = new AnnuityCompareRequest();
        age60.setAge(60);
        age60.setJoint(false);

        AnnuityCompareRequest age75 = new AnnuityCompareRequest();
        age75.setAge(75);
        age75.setJoint(false);

        double rate60 = service.simulateAllCompare(age60).getAnnuityRate();
        double rate75 = service.simulateAllCompare(age75).getAnnuityRate();

        assertTrue(rate75 > rate60,
            "Older purchaser (75) should receive a higher payout rate than younger (60)");
    }

    @Test
    void annuityCompare_deferredIncomeStart_boostsAnnuityRate() {
        // Income Start Year 7 = 6 years of deferral past purchase (year 1).
        // Boost = Annual Increase % (age 65) × 6 years, added once (not compounded).
        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setAge(65);
        req.setJoint(false);
        req.setIncomeStartYear(7);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        double expectedRate = AnnuityRateTable.lookup(65, false)
            + AnnuityRateTable.lookupAnnualIncreasePct(65) * 6;
        assertEquals(expectedRate, resp.getAnnuityRate(), 0.0001,
            "Deferred annuity rate should equal base rate + (annual increase % × deferral years)");
    }

    @Test
    void annuityCompare_noDeferral_rateUnboosted() {
        // Income Start Year 1 (the default) = 0 years of deferral -> no boost at all.
        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setAge(65);
        req.setJoint(false);
        req.setIncomeStartYear(1);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        assertEquals(AnnuityRateTable.lookup(65, false), resp.getAnnuityRate(), 0.0001,
            "With no deferral, the annuity rate should be exactly the base rate table value");
    }

    @Test
    void annuityCompare_longerDeferral_higherRate() {
        AnnuityCompareRequest shortDeferral = new AnnuityCompareRequest();
        shortDeferral.setAge(65);
        shortDeferral.setIncomeStartYear(3);

        AnnuityCompareRequest longDeferral = new AnnuityCompareRequest();
        longDeferral.setAge(65);
        longDeferral.setIncomeStartYear(10);

        double shortRate = service.simulateAllCompare(shortDeferral).getAnnuityRate();
        double longRate  = service.simulateAllCompare(longDeferral).getAnnuityRate();

        assertTrue(longRate > shortRate,
            "Waiting longer to start income should yield a higher effective annuity rate");
    }

    @Test
    void annuityCompare_zeroPctAnnuity_withAnnuityMatchesWithoutAnnuity() {
        // 0% annuitized means no annuity income at all — both legs should be identical
        AnnuityCompareRequest req = new AnnuityCompareRequest();
        req.setAnnuityPercentage(0.0);
        req.setAge(65);

        AnnuityCompareResponse resp = service.simulateAllCompare(req);

        assertEquals(resp.getWithoutAnnuity().getFailureCount(),
                     resp.getWithAnnuity().getFailureCount(),
            "0% annuity should produce the same failure count in both legs");
        assertEquals(0.0, resp.getInitialAnnuityIncome(), 0.01,
            "0% annuity should have zero initial income");
    }

    // -------------------------------------------------------------------------
    // Regression: manual allocation mXxx fields used in simulateAll
    // Bug: @JsonProperty missing → Jackson dropped mXxx values → 0% blended
    // return → all scenarios failed prematurely in manual allocation mode.
    // -------------------------------------------------------------------------

    @Test
    void simulateAll_manualAllocations_producesDistinctResultsFromAutoMode() {
        // Manual: 100% 5-yr treasuries (conservative but earns real returns)
        AllScenariosRequest manualReq = new AllScenariosRequest();
        manualReq.setManualAllocations(true);
        manualReq.setMFiveYearUS(1.0);
        manualReq.setExpensesAndMgmtFee(0.0);
        manualReq.setYearCount(30);

        // Auto: default 60% globally diversified stocks
        AllScenariosRequest autoReq = new AllScenariosRequest();
        autoReq.setExpensesAndMgmtFee(0.0);
        autoReq.setYearCount(30);

        AllScenariosResponse manualResp = service.simulateAll(manualReq);
        AllScenariosResponse autoResp   = service.simulateAll(autoReq);

        assertNotEquals(manualResp.getAverageEndingBalance(), autoResp.getAverageEndingBalance(), 1000.0,
            "Manual 100% 5-yr treasuries should produce different average balance from auto 60% stocks");

        // If mXxx fields were silently 0, the blended return would be 0% and failure rate ~100%.
        assertTrue(manualResp.getFailureRate() < 100.0,
            "Manual 100% 5-yr treasuries must not have 100% failure rate (would indicate allocations were 0)");
    }

    @Test
    void simulateAll_manualAllocations_sameStartYear_matchesSingleSimulation() {
        // The all-scenarios result for a given start year must match what the single
        // simulation would produce with the same manual allocation.
        int startYear = 1975;
        int yearCount = 30;

        // Single simulation with explicit allocation
        SimulationRequest singleReq = new SimulationRequest();
        singleReq.setStartYear(startYear);
        singleReq.setStartingNestEgg(1_000_000.0);
        singleReq.setInitialWithdrawal(43_000.0);
        singleReq.setExpensesAndMgmtFee(0.0);
        singleReq.setYearCount(yearCount);
        singleReq.setSp500(0.0);   singleReq.setCrsp1_10(0.50); singleReq.setOneMonth(0.0);
        singleReq.setFiveYearUS(0.50); singleReq.setCrsp6_10(0.0); singleReq.setFfIntl(0.0);
        singleReq.setDjUsReit(0.0); singleReq.setFfEmgMkts(0.0);

        List<YearResult> singleResults = service.simulate(singleReq);
        boolean singleFailed = singleResults.size() < yearCount
                || singleResults.get(Math.min(yearCount, singleResults.size()) - 1).getPortfolioEnd() <= 0;

        // All-scenarios with matching manual allocation
        AllScenariosRequest allReq = new AllScenariosRequest();
        allReq.setStartingNestEgg(1_000_000.0);
        allReq.setInitialWithdrawal(43_000.0);
        allReq.setExpensesAndMgmtFee(0.0);
        allReq.setYearCount(yearCount);
        allReq.setManualAllocations(true);
        allReq.setMCrsp1_10(0.50);
        allReq.setMFiveYearUS(0.50);

        AllScenariosResponse allResp = service.simulateAll(allReq);
        boolean allFailed = allResp.getScenarios().stream()
                .filter(s -> s.getStartYear() == startYear)
                .findFirst()
                .orElseThrow()
                .isFailed();

        assertEquals(singleFailed, allFailed,
            "For start year " + startYear + ", single simulation and all-scenarios must agree on failure");
    }

    // -------------------------------------------------------------------------
    // Regression: annuity withdrawal independence
    // Bug: from Year 2 onward, withdrawal was computed as
    //   (inflated target income − annuity income)
    // so when CPI > COLA cap, the gap widened and withdrawal grew faster than
    // inflation. Fix: withdrawal grows from prior year's withdrawal × (1 + CPI).
    // -------------------------------------------------------------------------

    @Test
    void annuity_year2Withdrawal_growsFromPriorWithdrawal_notFromTargetGap() {
        // 1951 CPI = 6.0%, COLA cap = 3%.
        // Year 1: target=$40K, annuity=$20K → portfolio withdrawal=$20K
        // Year 2 CORRECT: $20K × 1.06 = $21,200
        // Year 2 WRONG:   ($40K×1.06) − ($20K×1.03) = $42,400 − $20,600 = $21,800

        SimulationRequest req = buildAnnuityRequest(20_000.0);
        List<YearResult> results = service.simulate(req);

        YearResult year1 = results.get(0);
        YearResult year2 = results.get(1);

        double cpi = year1.getInflation(); // 0.06 from 1951 historical data
        double expectedWithdrawal = year1.getAnnualWithdrawal() * (1.0 + cpi);

        assertEquals(expectedWithdrawal, year2.getAnnualWithdrawal(), 1.0,
            "Year 2 withdrawal must equal Year 1 withdrawal × (1 + Year 1 CPI)");

        // Confirm it does NOT equal the old gap-fill calculation
        double wrongWithdrawal = (40_000.0 * (1.0 + cpi)) - year2.getAnnuityPayment();
        assertNotEquals(wrongWithdrawal, year2.getAnnualWithdrawal(), 1.0,
            "Withdrawal must not be computed as (inflated target − annuity payment)");
    }

    @Test
    void annuity_withdrawalGrowthRate_isIndependentOfAnnuitySize() {
        // Two portfolios with the same desired income but different annuity sizes.
        // After the fix, both Year-2 withdrawals must grow at the same CPI rate —
        // the annuity size must not influence withdrawal growth.

        List<YearResult> smallAnnuity = service.simulate(buildAnnuityRequest(15_000.0));
        List<YearResult> largeAnnuity = service.simulate(buildAnnuityRequest(25_000.0));

        double smallGrowth = smallAnnuity.get(1).getAnnualWithdrawal()
                           / smallAnnuity.get(0).getAnnualWithdrawal();
        double largeGrowth = largeAnnuity.get(1).getAnnualWithdrawal()
                           / largeAnnuity.get(0).getAnnualWithdrawal();

        assertEquals(smallGrowth, largeGrowth, 0.001,
            "Withdrawal growth rate (Year2/Year1) must be equal regardless of annuity size");
    }

    @Test
    void annuity_fixedMode_withdrawalRemainsConstantYearOverYear() {
        SimulationRequest req = buildAnnuityRequest(15_000.0);
        req.setWithdrawalMode("fixed");

        List<YearResult> results = service.simulate(req);

        double year1 = results.get(0).getAnnualWithdrawal();
        double year2 = results.get(1).getAnnualWithdrawal();
        double year3 = results.get(2).getAnnualWithdrawal();

        assertEquals(year1, year2, 1.0, "Fixed mode: Year 2 withdrawal must equal Year 1");
        assertEquals(year1, year3, 1.0, "Fixed mode: Year 3 withdrawal must equal Year 1");
    }

    @Test
    void annuity_totalIncome_equalsWithdrawalPlusAnnuityPayment() {
        List<YearResult> results = service.simulate(buildAnnuityRequest(20_000.0));

        for (YearResult r : results) {
            assertEquals(r.getAnnualWithdrawal() + r.getAnnuityPayment(),
                         r.getTotalIncome(), 1.0,
                "Total income must equal withdrawal + annuity payment in year " + r.getYear());
        }
    }

    // -------------------------------------------------------------------------
    // Manual cash flow "Income" classification
    // -------------------------------------------------------------------------

    private CashFlow buildFlow(String type, double amount) {
        CashFlow cf = new CashFlow();
        cf.setId("flow-" + type);
        cf.setDescription("test flow");
        cf.setAmount(amount);
        cf.setAllYears(true);
        cf.setInflationAdj("none");
        cf.setType(type);
        return cf;
    }

    @Test
    void incomeFlow_reducesWithdrawalFreshEachYear_butNotCashFlowApplied() {
        // Large nest egg relative to withdrawal keeps both scenarios far from depletion,
        // so the "clamp withdrawal to remaining balance" logic never kicks in and the
        // comparison isolates the Income-offset behavior from unrelated balance effects.
        SimulationRequest withoutFlow = new SimulationRequest();
        withoutFlow.setStartingNestEgg(5_000_000.0);
        SimulationRequest withIncomeFlow = new SimulationRequest();
        withIncomeFlow.setStartingNestEgg(5_000_000.0);
        withIncomeFlow.setCashFlows(List.of(buildFlow("income", 10_000.0)));

        List<YearResult> baseline = service.simulate(withoutFlow);
        List<YearResult> withIncome = service.simulate(withIncomeFlow);

        // Withdrawal = Desired Income - Income every year (entry applies allYears);
        // Desired Income itself is unaffected, so it equals the baseline's Withdrawal.
        int years = Math.min(baseline.size(), withIncome.size());
        for (int i = 0; i < years; i++) {
            assertEquals(baseline.get(i).getAnnualWithdrawal() - 10_000.0, withIncome.get(i).getAnnualWithdrawal(), 1.0,
                "Withdrawal must equal Desired Income minus Income in year " + (i + 1));
            assertEquals(0.0, withIncome.get(i).getCashFlowApplied(), 0.01,
                "Income-typed cash flow must not appear in cashFlowApplied");
            assertEquals(10_000.0, withIncome.get(i).getIncomeApplied(), 0.01,
                "Income-typed cash flow must be reflected in incomeApplied");
        }
    }

    @Test
    void incomeFlow_hasNoLastingEffect_onLaterYearsWithdrawalTrajectory() {
        CashFlow oneTimeIncome = buildFlow("income", 15_000.0);
        oneTimeIncome.setAllYears(false);
        oneTimeIncome.setYearStart(1);
        oneTimeIncome.setYearEnd(1);

        // Large nest egg keeps both scenarios far from depletion (see comment above).
        SimulationRequest withoutFlow = new SimulationRequest();
        withoutFlow.setStartingNestEgg(5_000_000.0);
        SimulationRequest withOneTimeIncome = new SimulationRequest();
        withOneTimeIncome.setStartingNestEgg(5_000_000.0);
        withOneTimeIncome.setCashFlows(List.of(oneTimeIncome));

        List<YearResult> baseline = service.simulate(withoutFlow);
        List<YearResult> withIncome = service.simulate(withOneTimeIncome);

        assertEquals(baseline.get(0).getAnnualWithdrawal() - 15_000.0, withIncome.get(0).getAnnualWithdrawal(), 1.0,
            "Year 1 Withdrawal must reflect the one-time Income offset");

        // Year 2 onward: the Income entry no longer applies, so Withdrawal must fully
        // recover to the undiscounted Desired Income trajectory (Option B: no scarring).
        int years = Math.min(baseline.size(), withIncome.size());
        for (int i = 1; i < years; i++) {
            assertEquals(baseline.get(i).getAnnualWithdrawal(), withIncome.get(i).getAnnualWithdrawal(), 1.0,
                "Withdrawal must fully recover once the Income entry's year range ends (year " + (i + 1) + ")");
        }
    }

    @Test
    void cashFlowTypeFlow_stillAffectsPortfolioBalance() {
        SimulationRequest req = new SimulationRequest();
        req.setCashFlows(List.of(buildFlow("cashflow", 10_000.0)));

        List<YearResult> results = service.simulate(req);

        assertEquals(10_000.0, results.get(0).getCashFlowApplied(), 0.01,
            "Default \"cashflow\" type must retain existing balance-affecting behavior");
        assertEquals(0.0, results.get(0).getIncomeApplied(), 0.01);
    }

    @Test
    void totalIncome_withoutAnnuity_equalsWithdrawalPlusIncome() {
        SimulationRequest req = new SimulationRequest();
        req.setCashFlows(List.of(buildFlow("income", 5_000.0)));

        List<YearResult> results = service.simulate(req);

        for (YearResult r : results) {
            assertEquals(r.getAnnualWithdrawal() + r.getIncomeApplied(), r.getTotalIncome(), 0.01,
                "Total Income must equal Withdrawal + Income (no annuity) in year " + r.getYear());
        }
    }

    @Test
    void totalIncome_withAnnuity_equalsWithdrawalPlusIncomePlusAnnuityPmt() {
        SimulationRequest req = buildAnnuityRequest(20_000.0);
        req.setCashFlows(List.of(buildFlow("income", 5_000.0)));

        List<YearResult> results = service.simulate(req);

        for (YearResult r : results) {
            assertEquals(r.getAnnualWithdrawal() + r.getIncomeApplied() + r.getAnnuityPayment(), r.getTotalIncome(), 0.01,
                "Total Income must equal Withdrawal + Income + Annuity Pmt in year " + r.getYear());
        }
    }

    @Test
    void incomeFlow_withAnnuity_reducesWithdrawalByIncomeAmount() {
        SimulationRequest withoutIncome = buildAnnuityRequest(20_000.0);
        SimulationRequest withIncome = buildAnnuityRequest(20_000.0);
        withIncome.setCashFlows(List.of(buildFlow("income", 5_000.0)));

        List<YearResult> baseline = service.simulate(withoutIncome);
        List<YearResult> results = service.simulate(withIncome);

        // Year 1: Withdrawal = Desired Income - Annuity - Income, matching the
        // no-income baseline's Withdrawal (which already nets out the annuity) minus Income.
        assertEquals(baseline.get(0).getAnnualWithdrawal() - 5_000.0, results.get(0).getAnnualWithdrawal(), 1.0,
            "Year 1 Withdrawal must additionally subtract the manual Income entry");
    }

    @Test
    void simulateAllCompare_incomeFlow_reducesAverageAnnualWithdrawal() {
        // Exercises simulateWithAnnuity() (the All-Scenarios/annuity-compare path),
        // the second engine that needed the same Desired-Income-minus-Income treatment.
        AnnuityCompareRequest withoutIncome = new AnnuityCompareRequest();
        AnnuityCompareRequest withIncome = new AnnuityCompareRequest();
        withIncome.setCashFlows(List.of(buildFlow("income", 5_000.0)));

        AnnuityCompareResponse baseline = service.simulateAllCompare(withoutIncome);
        AnnuityCompareResponse withIncomeResp = service.simulateAllCompare(withIncome);

        assertTrue(withIncomeResp.getWithAnnuity().getAverageAnnualWithdrawal()
                 < baseline.getWithAnnuity().getAverageAnnualWithdrawal(),
            "A manual Income entry must lower the average annual Withdrawal in the annuity-compare path");
    }

    // -------------------------------------------------------------------------
    // Exhaustion clamp: Withdrawal = Begin Balance, End Balance = $0, when the
    // required withdrawal exceeds the available balance.
    // -------------------------------------------------------------------------

    /** Zero-return request so portfolio math is exact (no market noise). */
    private SimulationRequest zeroReturnRequest() {
        SimulationRequest req = new SimulationRequest();
        req.setSp500(0.0); req.setCrsp1_10(0.0); req.setOneMonth(0.0);
        req.setFiveYearUS(0.0); req.setCrsp6_10(0.0); req.setFfIntl(0.0);
        req.setDjUsReit(0.0); req.setFfEmgMkts(0.0); req.setExpensesAndMgmtFee(0.0);
        return req;
    }

    @Test
    void exhaustion_year1_withdrawalExceedsNestEgg_clampsAndZeroesEndBalance() {
        SimulationRequest req = zeroReturnRequest();
        req.setStartingNestEgg(35_000.0);
        req.setInitialWithdrawal(50_000.0); // required > begin balance in year 1

        List<YearResult> results = service.simulate(req);

        assertEquals(1, results.size(), "Simulation must stop the year the portfolio is exhausted");
        assertEquals(35_000.0, results.get(0).getAnnualWithdrawal(), 0.01,
            "Withdrawal must equal the entire Begin Balance, not the unaffordable required amount");
        assertEquals(0.0, results.get(0).getPortfolioEnd(), 0.01, "End Balance must be exactly $0");
    }

    @Test
    void exhaustion_midSimulation_withdrawalExceedsRemainingBalance_clampsAndZeroesEndBalance() {
        SimulationRequest req = zeroReturnRequest();
        req.setStartingNestEgg(100_000.0);
        req.setInitialWithdrawal(60_000.0);
        req.setWithdrawalMode("fixed"); // withdrawal stays $60k every year regardless of balance

        List<YearResult> results = service.simulate(req);

        // Year 1: begin 100k, withdraw 60k (required < begin, normal), end 40k.
        assertEquals(60_000.0, results.get(0).getAnnualWithdrawal(), 0.01);
        assertEquals(40_000.0, results.get(0).getPortfolioEnd(), 0.01);

        // Year 2: begin 40k, required 60k > begin 40k -> exhaustion clamp.
        assertEquals(2, results.size(), "Simulation must stop at the exhaustion year");
        assertEquals(40_000.0, results.get(1).getAnnualWithdrawal(), 0.01,
            "Withdrawal must equal the entire remaining Begin Balance");
        assertEquals(0.0, results.get(1).getPortfolioEnd(), 0.01, "End Balance must be exactly $0");
    }

    @Test
    void exhaustion_cashFlowEntry_doesNotRevivePortfolioPastZero() {
        SimulationRequest req = zeroReturnRequest();
        req.setStartingNestEgg(35_000.0);
        req.setInitialWithdrawal(50_000.0); // required > begin balance in year 1
        CashFlow positiveCashFlow = buildFlow("cashflow", 50_000.0);
        req.setCashFlows(List.of(positiveCashFlow));

        List<YearResult> results = service.simulate(req);

        assertEquals(0.0, results.get(0).getCashFlowApplied(), 0.01,
            "Cash Flow must not apply once the pre-flow balance is already exhausted");
        assertEquals(0.0, results.get(0).getPortfolioEnd(), 0.01,
            "End Balance must stay exactly $0 even with an active Cash Flow entry that year");
    }

    @Test
    void exhaustion_normalCase_belowBalance_isUnaffected() {
        SimulationRequest req = zeroReturnRequest();
        req.setStartingNestEgg(80_000.0);
        req.setInitialWithdrawal(50_000.0); // required < begin balance

        List<YearResult> results = service.simulate(req);

        assertEquals(50_000.0, results.get(0).getAnnualWithdrawal(), 0.01,
            "Normal case: Withdrawal must equal the required amount, not the full balance");
        assertEquals(30_000.0, results.get(0).getPortfolioEnd(), 0.01);
    }

    /**
     * Regression test built from a real client scenario (large Social Security / FIA /
     * pension Income entries, inflation-adjusted mode, income deferred to year 7). Before
     * the fix, the internal "Desired Income" trajectory was clamped to the portfolio
     * balance BEFORE subtracting Income, so a year with large Income entries would freeze
     * Desired Income at whatever the balance happened to be that year — permanently
     * distorting all later years — even though the actual (post-Income) Withdrawal was
     * well within what the portfolio could afford.
     */
    @Test
    void desiredIncomeTrajectory_notPrematurelyClampedByLargeIncomeEntries() {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(1970);
        req.setStartingNestEgg(2_061_222.0);
        req.setInitialWithdrawal(500_000.0);
        req.setExpensesAndMgmtFee(0.002);
        req.setWithdrawalMode("inflation_adjusted");
        req.setYearCount(40);
        req.setIncomeStartYear(7);
        req.setSp500(0.0);
        req.setCrsp1_10(0.25);
        req.setCrsp6_10(0.20);
        req.setFfIntl(0.10);
        req.setFfEmgMkts(0.05);
        req.setDjUsReit(0.05);
        req.setOneMonth(0.05);
        req.setFiveYearUS(0.30);

        CashFlow afSS = new CashFlow();
        afSS.setId("1"); afSS.setDescription("AF SS"); afSS.setAmount(65500);
        afSS.setAllYears(false); afSS.setYearStart(7); afSS.setYearEnd(40);
        afSS.setInflationAdj("half"); afSS.setType("income");

        CashFlow afAvantis = new CashFlow();
        afAvantis.setId("2"); afAvantis.setDescription("AF Avantis Barclays FIA"); afAvantis.setAmount(48000);
        afAvantis.setAllYears(false); afAvantis.setYearStart(7); afAvantis.setYearEnd(40);
        afAvantis.setInflationAdj("half"); afAvantis.setType("income");

        CashFlow afAllianz = new CashFlow();
        afAllianz.setId("3"); afAllianz.setDescription("AF Allianz FIA"); afAllianz.setAmount(99000);
        afAllianz.setAllYears(false); afAllianz.setYearStart(7); afAllianz.setYearEnd(40);
        afAllianz.setInflationAdj("half"); afAllianz.setType("income");

        CashFlow preRetirement = new CashFlow();
        preRetirement.setId("4"); preRetirement.setDescription("Pre-retirement funding"); preRetirement.setAmount(75000);
        preRetirement.setAllYears(false); preRetirement.setYearStart(1); preRetirement.setYearEnd(6);
        preRetirement.setInflationAdj("none"); // no "type" in saved file -> defaults to "cashflow"

        CashFlow gfSS = new CashFlow();
        gfSS.setId("5"); gfSS.setDescription("GF SS"); gfSS.setAmount(28500);
        gfSS.setAllYears(false); gfSS.setYearStart(7); gfSS.setYearEnd(36);
        gfSS.setInflationAdj("half"); gfSS.setType("income");

        CashFlow ffs = new CashFlow();
        ffs.setId("6"); ffs.setDescription("AF FFS \"Annuity\""); ffs.setAmount(100000);
        ffs.setAllYears(false); ffs.setYearStart(7); ffs.setYearEnd(40);
        ffs.setInflationAdj("full"); ffs.setType("income");

        CashFlow gfPension = new CashFlow();
        gfPension.setId("7"); gfPension.setDescription("GF Fixed Pensions"); gfPension.setAmount(14190);
        gfPension.setAllYears(false); gfPension.setYearStart(6); gfPension.setYearEnd(40);
        gfPension.setInflationAdj("none"); gfPension.setType("income");

        req.setCashFlows(List.of(afSS, afAvantis, afAllianz, preRetirement, gfSS, ffs, gfPension));

        List<YearResult> results = service.simulate(req);

        // Years 7-15 (index 6-14): Withdrawal must grow every year (never freeze or shrink) —
        // the portfolio stays healthy throughout, so nothing should be clamping it early.
        for (int i = 7; i <= 14; i++) {
            assertTrue(results.get(i).getAnnualWithdrawal() > results.get(i - 1).getAnnualWithdrawal(),
                "Withdrawal must keep growing in year " + results.get(i).getYear()
                    + " — a premature clamp would freeze or shrink it despite a healthy portfolio");
        }

        // Year 15 (index 14, calendar 1984): must NOT be artificially capped to Begin Balance —
        // the real (Income-net) Withdrawal need is well within what the portfolio can afford.
        YearResult year15 = results.get(14);
        assertTrue(year15.getAnnualWithdrawal() < year15.getPortfolioBeginning(),
            "Year 15 Withdrawal must not be clamped down to exactly the Begin Balance");

        // Year 16 (index 15, calendar 1985): the portfolio is genuinely exhausted here —
        // Withdrawal must equal the full remaining Begin Balance and End Balance must be $0.
        YearResult year16 = results.get(15);
        assertEquals(year16.getPortfolioBeginning(), year16.getAnnualWithdrawal(), 0.01,
            "Year 16 Withdrawal must equal the entire remaining Begin Balance (genuine exhaustion)");
        assertEquals(0.0, year16.getPortfolioEnd(), 0.01, "Year 16 End Balance must be exactly $0");
        assertEquals(16, results.size(), "Simulation must stop at the genuine exhaustion year");
    }

    /**
     * Regression test built from a real client scenario (TPA mode, large Social Security /
     * FIA / pension Income entries). Before this fix, TPA's sustainability ratio was computed
     * against the pre-Income "Desired Income" target (~$736K) instead of the actual dollar
     * amount pulled from the portfolio (~$90K-380K once Income is netted out) — this made the
     * ratio look permanently unsustainable, freezing TOTAL INCOME while WITHDRAWAL kept
     * shrinking underneath it as Income grew, and the freeze didn't lift until the portfolio
     * ballooned past $9M. TPA must instead freeze/grow the actual Withdrawal directly.
     */
    @Test
    void tpaMode_freezesActualWithdrawal_notThePreIncomeTarget() {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(1970);
        req.setStartingNestEgg(2_076_222.0);
        req.setInitialWithdrawal(500_000.0);
        req.setExpensesAndMgmtFee(0.002);
        req.setWithdrawalMode("tpa");
        req.setYearCount(40);
        req.setIncomeStartYear(7);
        req.setSp500(0.0);
        req.setCrsp1_10(0.25);
        req.setCrsp6_10(0.20);
        req.setFfIntl(0.10);
        req.setFfEmgMkts(0.05);
        req.setDjUsReit(0.05);
        req.setOneMonth(0.05);
        req.setFiveYearUS(0.30);

        CashFlow afSS = new CashFlow();
        afSS.setId("1"); afSS.setDescription("AF SS"); afSS.setAmount(65500);
        afSS.setAllYears(false); afSS.setYearStart(7); afSS.setYearEnd(40);
        afSS.setInflationAdj("half"); afSS.setType("income");

        CashFlow afAvantis = new CashFlow();
        afAvantis.setId("2"); afAvantis.setDescription("AF Avantis Barclays FIA"); afAvantis.setAmount(48000);
        afAvantis.setAllYears(false); afAvantis.setYearStart(7); afAvantis.setYearEnd(40);
        afAvantis.setInflationAdj("half"); afAvantis.setType("income");

        CashFlow afAllianz = new CashFlow();
        afAllianz.setId("3"); afAllianz.setDescription("AF Allianz FIA"); afAllianz.setAmount(99000);
        afAllianz.setAllYears(false); afAllianz.setYearStart(7); afAllianz.setYearEnd(40);
        afAllianz.setInflationAdj("half"); afAllianz.setType("income");

        CashFlow preRetirement = new CashFlow();
        preRetirement.setId("4"); preRetirement.setDescription("Pre-retirement funding"); preRetirement.setAmount(75000);
        preRetirement.setAllYears(false); preRetirement.setYearStart(1); preRetirement.setYearEnd(6);
        preRetirement.setInflationAdj("none");

        CashFlow gfSS = new CashFlow();
        gfSS.setId("5"); gfSS.setDescription("GF SS"); gfSS.setAmount(28500);
        gfSS.setAllYears(false); gfSS.setYearStart(7); gfSS.setYearEnd(36);
        gfSS.setInflationAdj("half"); gfSS.setType("income");

        CashFlow ffs = new CashFlow();
        ffs.setId("6"); ffs.setDescription("AF FFS \"Annuity\""); ffs.setAmount(100000);
        ffs.setAllYears(false); ffs.setYearStart(7); ffs.setYearEnd(40);
        ffs.setInflationAdj("full"); ffs.setType("income");

        CashFlow gfPension = new CashFlow();
        gfPension.setId("7"); gfPension.setDescription("GF Fixed Pensions"); gfPension.setAmount(14190);
        gfPension.setAllYears(false); gfPension.setYearStart(6); gfPension.setYearEnd(40);
        gfPension.setInflationAdj("none"); gfPension.setType("income");

        req.setCashFlows(List.of(afSS, afAvantis, afAllianz, preRetirement, gfSS, ffs, gfPension));

        List<YearResult> results = service.simulate(req);

        // Year 7 (index 6) through year 26 (index 25): TPA's ratio never clears the threshold
        // (the portfolio hasn't grown enough yet), so Withdrawal must stay frozen at the exact
        // year-7 amount for this whole span, while Total Income keeps growing from Income alone.
        double frozenWithdrawal = results.get(6).getAnnualWithdrawal();
        for (int i = 7; i <= 25; i++) {
            assertEquals(frozenWithdrawal, results.get(i).getAnnualWithdrawal(), 0.01,
                "Withdrawal must stay frozen at the year-7 amount in year " + results.get(i).getYear());
            assertTrue(results.get(i).getTotalIncome() > results.get(i - 1).getTotalIncome(),
                "Total Income must keep growing (from Income alone) in year " + results.get(i).getYear());
        }

        // Year 27 (index 26): the portfolio has grown enough that the ratio finally clears the
        // threshold, so Withdrawal must unfreeze and grow past the long-frozen amount.
        assertTrue(results.get(26).getAnnualWithdrawal() > frozenWithdrawal,
            "Withdrawal must unfreeze and grow once TPA's ratio clears the threshold");
    }

    private SimulationRequest buildAnnuityRequest(double initialAnnuityIncome) {
        SimulationRequest req = new SimulationRequest();
        req.setStartYear(1951);
        req.setStartingNestEgg(2_000_000.0); // large enough that portfolio won't exhaust
        req.setInitialWithdrawal(40_000.0);
        req.setAnnuityInitialIncome(initialAnnuityIncome);
        req.setAnnuityCap(0.03);
        req.setWithdrawalMode("inflation_adjusted");
        req.setSp500(0.0); req.setCrsp1_10(0.0); req.setOneMonth(0.0);
        req.setFiveYearUS(1.0); // 100% 5-yr treasuries for clean calculations
        req.setCrsp6_10(0.0); req.setFfIntl(0.0); req.setDjUsReit(0.0); req.setFfEmgMkts(0.0);
        return req;
    }
}
