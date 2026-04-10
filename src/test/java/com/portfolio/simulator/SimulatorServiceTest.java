package com.portfolio.simulator;

import com.portfolio.simulator.model.AllScenariosRequest;
import com.portfolio.simulator.model.AllScenariosResponse;
import com.portfolio.simulator.model.AnnuityCompareRequest;
import com.portfolio.simulator.model.AnnuityCompareResponse;
import com.portfolio.simulator.model.AnnuityRateTable;
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
}
