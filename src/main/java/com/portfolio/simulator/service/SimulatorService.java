package com.portfolio.simulator.service;

import com.portfolio.simulator.model.AllScenariosRequest;
import com.portfolio.simulator.model.AllScenariosResponse;
import com.portfolio.simulator.model.AnnuityCompareRequest;
import com.portfolio.simulator.model.AnnuityCompareResponse;
import com.portfolio.simulator.model.AnnuityRateTable;
import com.portfolio.simulator.model.ScenarioSummary;
import com.portfolio.simulator.model.SimulationRequest;
import com.portfolio.simulator.model.YearResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Core simulation engine.
 *
 * Historical data is loaded from the hardcoded defaults on startup.
 * If a spreadsheet has previously been uploaded it is loaded from disk instead,
 * overriding the defaults.  Call loadFromSpreadsheet() to update at runtime.
 */
@Service
public class SimulatorService {

    private static final Logger log = Logger.getLogger(SimulatorService.class.getName());

    private final SpreadsheetLoaderService loader;

    @Value("${app.data.path:./data/returns.xlsx}")
    private String dataFilePath;

    // Instance variable — replaced when a new spreadsheet is uploaded
    private Map<Integer, double[]> historicalData = buildDefaultData();

    public SimulatorService(SpreadsheetLoaderService loader) {
        this.loader = loader;
    }

    /**
     * On startup, try to load from the persisted spreadsheet on disk.
     * Falls back silently to the hardcoded defaults if no file exists yet.
     */
    @PostConstruct
    void tryLoadFromDisk() {
        Path path = Paths.get(dataFilePath);
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                historicalData = Collections.unmodifiableMap(loader.load(is));
                log.info("Loaded historical data from " + path + " (" + historicalData.size() + " years)");
            } catch (IOException e) {
                log.warning("Could not load spreadsheet from disk (" + path + "), using defaults. Error: " + e.getMessage());
            }
        }
    }

    /**
     * Replaces historical data with values parsed from the given spreadsheet stream.
     * Also saves the file to disk so the data survives a server restart.
     *
     * @param inputStream  raw bytes of the uploaded .xlsx / .xltm file
     * @param fileBytes    same bytes, for persistence to disk
     * @return the number of years loaded
     */
    public int loadFromSpreadsheet(InputStream inputStream, byte[] fileBytes) throws IOException {
        Map<Integer, double[]> newData = loader.load(inputStream);
        historicalData = Collections.unmodifiableMap(newData);

        // Persist to disk
        Path path = Paths.get(dataFilePath);
        Files.createDirectories(path.getParent());
        Files.write(path, fileBytes);
        log.info("Saved uploaded spreadsheet to " + path + " (" + newData.size() + " years)");

        return newData.size();
    }

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    public List<YearResult> simulate(SimulationRequest req) {
        List<YearResult> results = new ArrayList<>();
        int maxYear = Collections.max(historicalData.keySet());

        for (int seq = 1; ; seq++) {
            int year = req.getStartYear() + (seq - 1);
            if (year > maxYear) break;

            double[] row = historicalData.get(year);
            if (row == null) break;

            YearResult r = new YearResult();
            r.setSequenceNumber(seq);
            r.setYear(year);
            r.setInflation(row[0]);

            if (seq == 1) {
                // --- First year ---
                // D6 = startingNestEgg, E6 = initialWithdrawal
                r.setPortfolioBeginning(req.getStartingNestEgg());
                r.setAnnualWithdrawal(req.getInitialWithdrawal());
                double rate = computeBlendedReturn(year, req);
                r.setPortfolioReturnRate(rate);

                // G6: IF((D6>0), F6*(D6-E6), 0)
                double gain = (r.getPortfolioBeginning() > 0)
                    ? rate * (r.getPortfolioBeginning() - r.getAnnualWithdrawal())
                    : 0.0;
                r.setPortfolioReturnDollars(gain);
                r.setTotalIncome(r.getAnnualWithdrawal());
                // I6: D6-E6+G6
                r.setPortfolioEnd(r.getPortfolioBeginning() - r.getAnnualWithdrawal() + gain);

            } else {
                // --- Subsequent years ---
                // E7: MIN( IF( OR(prevSeq>0, prevCpi<0) AND (prevEnd<>0),
                //               prevWithdraw*(1+prevCpi),
                //               IF(prevEnd=0, 0, prevWithdraw) ),
                //          beginningBalance )
                YearResult prev = results.get(seq - 2);

                r.setPortfolioBeginning(prev.getPortfolioEnd());

                boolean condOR  = (prev.getSequenceNumber() > 0) || (prev.getInflation() < 0);
                boolean condAND = (prev.getPortfolioEnd() != 0);
                double inflationAdjWithdrawal;
                if (condOR && condAND) {
                    inflationAdjWithdrawal = prev.getAnnualWithdrawal() * (1 + prev.getInflation());
                } else {
                    inflationAdjWithdrawal = (prev.getPortfolioEnd() == 0) ? 0.0 : prev.getAnnualWithdrawal();
                }
                // Withdrawal cannot exceed beginning balance
                r.setAnnualWithdrawal(Math.min(inflationAdjWithdrawal, r.getPortfolioBeginning()));

                double rate = computeBlendedReturn(year, req);
                r.setPortfolioReturnRate(rate);

                // G7: IF((D7>0), F7*(D7-E7), 0)
                double gain = (r.getPortfolioBeginning() > 0)
                    ? rate * (r.getPortfolioBeginning() - r.getAnnualWithdrawal())
                    : 0.0;
                r.setPortfolioReturnDollars(gain);
                r.setTotalIncome(r.getAnnualWithdrawal());
                r.setPortfolioEnd(r.getPortfolioBeginning() - r.getAnnualWithdrawal() + gain);
            }

            results.add(r);

            if (r.getPortfolioEnd() <= 0) break;
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // ALL SCENARIOS  (runs all N-year windows from 1929 to lastStartYear)
    // -------------------------------------------------------------------------

    private static final int FIRST_START_YEAR = 1929;

    public AllScenariosResponse simulateAll(AllScenariosRequest req) {
        int scenarioYears = req.getYearCount();

        // Build a SimulationRequest from the caller's allocation & fee settings
        SimulationRequest base = new SimulationRequest();
        base.setStartingNestEgg(req.getStartingNestEgg());
        base.setInitialWithdrawal(req.getInitialWithdrawal());
        base.setExpensesAndMgmtFee(req.getExpensesAndMgmtFee());
        base.setSp500(req.getSp500());
        base.setCrsp1_10(req.getCrsp1_10());
        base.setOneMonth(req.getOneMonth());
        base.setFiveYearUS(req.getFiveYearUS());
        base.setCrsp6_10(req.getCrsp6_10());
        base.setFfIntl(req.getFfIntl());
        base.setDjUsReit(req.getDjUsReit());
        base.setFfEmgMkts(req.getFfEmgMkts());

        int maxYear = Collections.max(historicalData.keySet());
        int lastStartYear = maxYear - scenarioYears + 1; // last year with full N-yr data

        List<ScenarioSummary> scenarios = new ArrayList<>();
        int failureCount = 0;
        int earliestFailureYears = Integer.MAX_VALUE;
        double highestEndingBalance = 0;
        double totalEndingBalance = 0;
        int survivorCount = 0;
        int worstStartYear = FIRST_START_YEAR;
        int bestStartYear = FIRST_START_YEAR;
        double worstBalance = Double.MAX_VALUE;
        double bestBalance = Double.MIN_VALUE;

        for (int startYear = FIRST_START_YEAR; startYear <= lastStartYear; startYear++) {
            base.setStartYear(startYear);
            List<YearResult> results = simulate(base);

            // Cap at scenarioYears
            List<YearResult> window = results.size() > scenarioYears
                ? results.subList(0, scenarioYears)
                : results;

            ScenarioSummary s = new ScenarioSummary();
            s.setStartYear(startYear);

            boolean failed = window.size() < scenarioYears
                || window.get(window.size() - 1).getPortfolioEnd() <= 0;
            s.setFailed(failed);
            s.setYearsSurvived(window.size());

            double endBalance = failed ? 0 : window.get(window.size() - 1).getPortfolioEnd();
            s.setEndingBalance(endBalance);
            scenarios.add(s);

            if (failed) {
                failureCount++;
                earliestFailureYears = Math.min(earliestFailureYears, window.size());
                if (endBalance < worstBalance) { worstBalance = endBalance; worstStartYear = startYear; }
            } else {
                totalEndingBalance += endBalance;
                survivorCount++;
                if (endBalance > highestEndingBalance) highestEndingBalance = endBalance;
                if (endBalance > bestBalance) { bestBalance = endBalance; bestStartYear = startYear; }
                if (endBalance < worstBalance) { worstBalance = endBalance; worstStartYear = startYear; }
            }
        }

        AllScenariosResponse resp = new AllScenariosResponse();
        resp.setScenarios(scenarios);
        resp.setTotalScenarios(scenarios.size());
        resp.setFailureCount(failureCount);
        resp.setFailureRate(Math.round((failureCount * 100.0 / scenarios.size()) * 10.0) / 10.0);
        resp.setEarliestFailureYears(earliestFailureYears == Integer.MAX_VALUE ? 0 : earliestFailureYears);
        resp.setHighestEndingBalance(highestEndingBalance);
        resp.setAverageEndingBalance(survivorCount > 0 ? totalEndingBalance / survivorCount : 0);
        resp.setWorstStartYear(worstStartYear);
        resp.setBestStartYear(bestStartYear);
        resp.setYearCount(scenarioYears);
        return resp;
    }

    // -------------------------------------------------------------------------
    // BLENDED RETURN
    // Mirrors column K formula in "Advanced Returns":
    //   K = (sp500 × SP500)
    //     + (crsp1_10 × CRSP1_10)
    //     + (oneMonth × T-Bills)
    //     + (fiveYearUS × 5YrTreas)
    //     + (crsp6_10 × CRSP6_10)
    //     + (ffIntl × FFIntl)
    //     + (djUsReit × REIT)
    //     + (ffEmgMkts × EmgMkts)
    //     - expensesAndMgmtFee
    // -------------------------------------------------------------------------
    private double computeBlendedReturn(int year, SimulationRequest req) {
        double[] row = historicalData.get(year);
        if (row == null) return -req.getExpensesAndMgmtFee();

        double sp500    = row[1];
        double crsp1_10 = row[2];
        double tBills   = row[3];
        double treas5yr = row[4];
        double crsp6_10 = row[5];
        double ffIntl   = row[6];
        double reit     = row[7];
        double emgMkts  = row[8];

        return (req.getSp500()      * sp500)
             + (req.getCrsp1_10()   * crsp1_10)
             + (req.getOneMonth()   * tBills)
             + (req.getFiveYearUS() * treas5yr)
             + (req.getCrsp6_10()   * crsp6_10)
             + (req.getFfIntl()     * ffIntl)
             + (req.getDjUsReit()   * reit)
             + (req.getFfEmgMkts()  * emgMkts)
             - req.getExpensesAndMgmtFee();
    }

    // -------------------------------------------------------------------------
    // DEFAULT HISTORICAL DATA — used until a spreadsheet is uploaded
    // (from "Advanced Returns" sheet, columns A–J)
    // Key   = year
    // Value = double[] { CPI, SP500, CRSP1_10, T-Bills(1mo), 5YrTreasuries, CRSP6_10, FFIntl, REIT, EmgMkts }
    // FFIntl/REIT/EmgMkts use CRSP1_10 as proxy for years before real data existed (~1975+)
    // -------------------------------------------------------------------------
    private static Map<Integer, double[]> buildDefaultData() {
        Map<Integer, double[]> data = new HashMap<>();
        data.put(1929, new double[]{ -0.0058, -0.3119, -0.3168, 0.0156, 0.0382, -0.3795, -0.3168, -0.3168, -0.3168 });
        data.put(1930, new double[]{ -0.0639534872551056, -0.248950061276622, -0.287990281699459, 0.0241034069838444, 0.067151105314349, -0.399295399634056, -0.287990281699459, -0.287990281699459, -0.287990281699459 });
        data.put(1931, new double[]{ -0.0931677017713394, -0.433485834389562, -0.435242334097107, 0.0107302656053732, -0.0232016139970406, -0.501937518941534, -0.435242334097107, -0.435242334097107, -0.435242334097107 });
        data.put(1932, new double[]{ -0.102739726098865, -0.0819889433459114, -0.0862439407785889, 0.00961727928086287, 0.0881120571705954, -0.0219058898301721, -0.0862439407785889, -0.0862439407785889, -0.0862439407785889 });
        data.put(1933, new double[]{ 0.00763358734609909, 0.539700780152496, 0.566514038249488, 0.00297458494476444, 0.0182708997212342, 1.24665139345477, 0.566514038249488, 0.566514038249488, 0.566514038249488 });
        data.put(1934, new double[]{ 0.015151516049303, -0.0143073320033779, 0.0408923626333539, 0.00162813092957403, 0.0899635023733998, 0.221080465438252, 0.0408923626333539, 0.0408923626333539, 0.0408923626333539 });
        data.put(1935, new double[]{ 0.0298507456072412, 0.47656357234639, 0.444313103395225, 0.00168629565580947, 0.0700661825303834, 0.591905085144078, 0.444313103395225, 0.444313103395225, 0.444313103395225 });
        data.put(1936, new double[]{ 0.0144927539454549, 0.339224020976951, 0.323182484635028, 0.00177441615910023, 0.0305697783361889, 0.563082507230172, 0.323182484635028, 0.323182484635028, 0.323182484635028 });
        data.put(1937, new double[]{ 0.0285714289621817, -0.350227601202219, -0.347362671872794, 0.00307920207578105, 0.015581317292904, -0.490546747864878, -0.347362671872794, -0.347362671872794, -0.347362671872794 });
        data.put(1938, new double[]{ -0.027777778830841, 0.311368112790911, 0.281629905480163, -0.000162215781152497, 0.0623030475364112, 0.404051662763712, 0.281629905480163, 0.281629905480163, 0.281629905480163 });
        data.put(1939, new double[]{ -1.10162634570088e-09, -0.0042237308645644, 0.0282556984709668, 0.000204994109742618, 0.0452438524431764, 0.0372196374444105, 0.0282556984709668, 0.0282556984709668, 0.0282556984709668 });
        data.put(1940, new double[]{ 0.0071428569732801, -0.0977883629758554, -0.0709463243204035, 4.79789782639184e-05, 0.0296141256064264, -0.0593063701582961, -0.0709463243204035, -0.0709463243204035, -0.0709463243204035 });
        data.put(1941, new double[]{ 0.0992907804568053, -0.115766722348485, -0.100780292232219, 0.000597109930394302, 0.00495020808437063, -0.104176858947881, -0.100780292232219, -0.100780292232219, -0.100780292232219 });
        data.put(1942, new double[]{ 0.0903225806656451, 0.203329775128291, 0.161224960199017, 0.00268024766937658, 0.0193632382825721, 0.284977943863029, 0.161224960199017, 0.161224960199017, 0.161224960199017 });
        data.put(1943, new double[]{ 0.0295857984396677, 0.259075922153561, 0.284267189871869, 0.00347150958948239, 0.0281003668220834, 0.636430013345024, 0.284267189871869, 0.284267189871869, 0.284267189871869 });
        data.put(1944, new double[]{ 0.0229885059027848, 0.197299025726789, 0.214907383492044, 0.0033049925731794, 0.0179523619557436, 0.452643019952508, 0.214907383492044, 0.214907383492044, 0.214907383492044 });
        data.put(1945, new double[]{ 0.0224719103542372, 0.364122472215439, 0.384956337448742, 0.00329596336239946, 0.0221894308585813, 0.673251597843468, 0.384956337448742, 0.384956337448742, 0.384956337448742 });
        data.put(1946, new double[]{ 0.181318682159817, -0.0807432501793661, -0.0617405233424988, 0.00353069966921771, 0.0100453335843578, -0.113265818041504, -0.0617405233424988, -0.0617405233424988, -0.0617405233424988 });
        data.put(1947, new double[]{ 0.0883720929073302, 0.0569655815016505, 0.0358488941190382, 0.00503437192809253, 0.00910961759414186, -0.0305597856168388, 0.0358488941190382, 0.0358488941190382, 0.0358488941190382 });
        data.put(1948, new double[]{ 0.0299145311675617, 0.0551082249529329, 0.0210895574090804, 0.00811175407460141, 0.0184762636032856, -0.0458690611469902, 0.0210895574090804, 0.0210895574090804, 0.0210895574090804 });
        data.put(1949, new double[]{ -0.0207468872006695, 0.187862140008587, 0.20217960398737, 0.0110263197845424, 0.0232310302416394, 0.213156156208004, 0.20217960398737, 0.20217960398737, 0.20217960398737 });
        data.put(1950, new double[]{ 0.059322032928727, 0.317408529778294, 0.296104961028788, 0.0119559737907442, 0.00701235498243591, 0.382112294105359, 0.296104961028788, 0.296104961028788, 0.296104961028788 });
        data.put(1951, new double[]{ 0.0600000019815019, 0.240159419064956, 0.20681888890524, 0.0149280626788948, 0.00361983757340956, 0.144073973313171, 0.20681888890524, 0.20681888890524, 0.20681888890524 });
        data.put(1952, new double[]{ 0.00754716965116953, 0.18351009400481, 0.134165604655541, 0.0165601266983797, 0.0163317382958543, 0.0909311530115771, 0.134165604655541, 0.134165604655541, 0.134165604655541 });
        data.put(1953, new double[]{ 0.00749063610154899, -0.00975132996013883, 0.006714837935901, 0.0182371852832297, 0.0323236356937355, -0.0341959886537327, 0.006714837935901, 0.006714837935901, 0.006714837935901 });
        data.put(1954, new double[]{ -0.00743494399878719, 0.526222003007204, 0.499777345013846, 0.00863883465737603, 0.0268241133092539, 0.587144684260463, 0.499777345013846, 0.499777345013846, 0.499777345013846 });
        data.put(1955, new double[]{ 0.00374531783626408, 0.315391373140896, 0.252141678834142, 0.015736551358245, -0.0065048765842497, 0.2100893638461, 0.252141678834142, 0.252141678834142, 0.252141678834142 });
        data.put(1956, new double[]{ 0.0298507459779411, 0.0655537453592012, 0.0826477450600504, 0.0245924537771571, -0.00421089806401409, 0.0603162664431851, 0.0826477450600504, 0.0826477450600504, 0.0826477450600504 });
        data.put(1957, new double[]{ 0.0289855066298947, -0.107914266401983, -0.100469022990861, 0.0313905390802318, 0.0783852835501633, -0.173862227229936, -0.100469022990861, -0.100469022990861, -0.100469022990861 });
        data.put(1958, new double[]{ 0.0176056345727504, 0.433716443081126, 0.450219917966576, 0.0154147139647338, -0.0128849734156832, 0.633303628473494, 0.450219917966576, 0.450219917966576, 0.450219917966576 });
        data.put(1959, new double[]{ 0.0173010389136377, 0.119768462381524, 0.126686827044764, 0.0295196627982097, -0.00390635835106634, 0.174971795559865, 0.126686827044764, 0.126686827044764, 0.126686827044764 });
        data.put(1960, new double[]{ 0.0136054426290018, 0.00464298138397168, 0.011584243024978, 0.0266273892290787, 0.117561462866872, -0.0363929067559212, 0.011584243024978, 0.011584243024978, 0.011584243024978 });
        data.put(1961, new double[]{ 0.00671140973266593, 0.268861031225499, 0.269457975115085, 0.0212652628000469, 0.0184925752027389, 0.29719374014582, 0.269457975115085, 0.269457975115085, 0.269457975115085 });
        data.put(1962, new double[]{ 0.0133333325448486, -0.0872777292139638, -0.101754861880897, 0.0273363345128017, 0.0556410874682753, -0.173694071555572, -0.101754861880897, -0.101754861880897, -0.101754861880897 });
        data.put(1963, new double[]{ 0.0164473691282136, 0.227756746064169, 0.209779634208491, 0.0311651992766673, 0.0164070809795358, 0.165573238435953, 0.209779634208491, 0.209779634208491, 0.209779634208491 });
        data.put(1964, new double[]{ 0.00970873700115771, 0.165077291370179, 0.161259333314641, 0.0353571900764573, 0.0404336630034508, 0.17095976786232, 0.161259333314641, 0.161259333314641, 0.161259333314641 });
        data.put(1965, new double[]{ 0.0192307689961404, 0.124523160023627, 0.144630822186298, 0.0392723505369161, 0.0101842780670636, 0.360139931757527, 0.144630822186298, 0.144630822186298, 0.144630822186298 });
        data.put(1966, new double[]{ 0.034591195111199, -0.100478095861859, -0.0874026515241073, 0.0475931167271655, 0.0468808555783076, -0.0746642533199042, -0.0874026515241073, -0.0874026515241073, -0.0874026515241073 });
        data.put(1967, new double[]{ 0.0303951368280846, 0.239871105754691, 0.287380379414093, 0.0421000772196323, 0.0100876589378578, 0.763574172642738, 0.287380379414093, 0.287380379414093, 0.287380379414093 });
        data.put(1968, new double[]{ 0.0471976413637301, 0.110814211072406, 0.141415239945543, 0.0520579363250742, 0.0453501584645282, 0.379404675594467, 0.141415239945543, 0.141415239945543, 0.141415239945543 });
        data.put(1969, new double[]{ 0.0619718307390991, -0.0848615153403149, -0.109136342929965, 0.0658380718408564, -0.00736647095543741, -0.257882653664172, -0.109136342929965, -0.109136342929965, -0.109136342929965 });
        data.put(1970, new double[]{ 0.0557029174890977, 0.0402601462724581, 2.30808872938582e-05, 0.0652497050362031, 0.16858845416212, -0.122118950422231, 2.30808872938582e-05, 2.30808872938582e-05, 2.30808872938582e-05 });
        data.put(1971, new double[]{ 0.0326633166104562, 0.143179458097906, 0.161458204932623, 0.0438612806370726, 0.0872070471647404, 0.194807588857274, 0.161458204932623, 0.161458204932623, 0.161458204932623 });
        data.put(1972, new double[]{ 0.0340632612901097, 0.189759135626222, 0.168371784419832, 0.0383995263059369, 0.0515956397655866, 0.0325947399845636, 0.168371784419832, 0.168371784419832, 0.168371784419832 });
        data.put(1973, new double[]{ 0.0870588234111724, -0.146659442289406, -0.18064487899065, 0.069303142605702, 0.0360556479924221, -0.364691266422588, -0.18064487899065, -0.18064487899065, -0.18064487899065 });
        data.put(1974, new double[]{ 0.123376622489003, -0.264582756753376, -0.270373038403411, 0.0800305884202763, 0.071876376196335, -0.261340186984807, -0.270373038403411, -0.270373038403411, -0.270373038403411 });
        data.put(1975, new double[]{ 0.0693641616850349, 0.37211960760346, 0.387533639326003, 0.0580359466802527, 0.0803357676528913, 0.639210339591356, 0.362708167506054, 0.387533639326003, 0.387533639326003 });
        data.put(1976, new double[]{ 0.0486486500221821, 0.238489371146422, 0.267612232226168, 0.0508251603349714, 0.113990352312172, 0.514662066522774, 0.0417430694594207, 0.267612232226168, 0.267612232226168 });
        data.put(1977, new double[]{ 0.0670103094496774, -0.0717930128844977, -0.0425961120408163, 0.0512021148962267, 0.0290910657192618, 0.183839781211706, 0.182021793171762, -0.0425961120408163, -0.0425961120408163 });
        data.put(1978, new double[]{ 0.0901771342634503, 0.0657395947838273, 0.074865063121758, 0.0718082326871996, 0.0244035305363004, 0.183334306141868, 0.329294343730334, 0.109799999995222, 0.074865063121758 });
        data.put(1979, new double[]{ 0.132939436623745, 0.184239764922035, 0.226242878727847, 0.103762301903503, 0.0691714358759532, 0.455907106973593, 0.0957874778023333, 0.489908091417133, 0.226242878727847 });
        data.put(1980, new double[]{ 0.125162971648185, 0.324076851501436, 0.328143362798177, 0.112355116955396, 0.0710407519530041, 0.334591232654648, 0.235444407628881, 0.331176292645457, 0.328143362798177 });
        data.put(1981, new double[]{ 0.0892236400825219, -0.049088144783707, -0.0364839543669018, 0.147088625966817, 0.10925033324919, 0.0422293639092492, -0.0168242759977285, 0.178819681192536, -0.0364839543669018 });
        data.put(1982, new double[]{ 0.03829787095734, 0.214094274938087, 0.210005550880346, 0.105430854865416, 0.249865394134456, 0.288399734657231, -0.0249265896618986, 0.209118588022695, 0.210005550880346 });
        data.put(1983, new double[]{ 0.0379098366487054, 0.225138135911181, 0.219754452603502, 0.0879832226305428, 0.0805874303900434, 0.299479155591498, 0.23624838832351, 0.321709750509652, 0.219754452603502 });
        data.put(1984, new double[]{ 0.0394866726255714, 0.062663224413356, 0.0451102837722452, 0.0984938733704892, 0.142488779398523, -0.0556280208435542, 0.0637258691287166, 0.218854000882363, 0.0451102837722452 });
        data.put(1985, new double[]{ 0.0379867050632505, 0.321706866518365, 0.321681998492923, 0.0772329519628136, 0.178085788199369, 0.3134208029607, 0.528309244462494, 0.0649967353552940, 0.321681998492923 });
        data.put(1986, new double[]{ 0.0109789573413721, 0.184705266701603, 0.161904921199843, 0.0616243430970076, 0.130460784252076, 0.0710045572750093, 0.668927652641882, 0.197451046174581, 0.161904921199843 });
        data.put(1987, new double[]{ 0.0443438927222688, 0.0523075800748809, 0.0167012168271128, 0.0546559566650471, 0.0359818989571818, -0.0909862134993626, 0.254257242352926, -0.0659384988024546, 0.0167012168271128 });
        data.put(1988, new double[]{ 0.0441941071219638, 0.168092926389822, 0.1802791196717, 0.0634780614161086, 0.062698694905978, 0.239108684907263, 0.246883039850923, 0.174822271042127, 0.1802791196717 });
        data.put(1989, new double[]{ 0.0464730281465993, 0.314908447912292, 0.28863511170143, 0.0837042615754458, 0.126858726698884, 0.160925856702053, 0.112095572510407, 0.0271600051523451, 0.28863511170143 });
        data.put(1990, new double[]{ 0.06106265016586, -0.0310375912638435, -0.0595848076704465, 0.0781346982163191, 0.0945124732866602, -0.202285591911274, -0.219696434321033, -0.234425326864151, -0.0542999869912134 });
        data.put(1991, new double[]{ 0.0306427508122256, 0.304653911105353, 0.346669400697819, 0.0559527028200806, 0.141029871477951, 0.488744514311581, 0.121378559288932, 0.238371779504474, 0.372160762210196 });
        data.put(1992, new double[]{ 0.0290065257589534, 0.076247831874581, 0.0979741321082779, 0.0350630703122445, 0.0693721122812383, 0.195206117518097, -0.127668947955487, 0.151342247788332, 0.0402782561761277 });
        data.put(1993, new double[]{ 0.0274841427157018, 0.100722516124989, 0.111415475893184, 0.0289694601209223, 0.0822259429042076, 0.186882276151699, 0.313398245276299, 0.151372618364704, 0.892634504365248 });
        data.put(1994, new double[]{ 0.0267489730367003, 0.0132012395753931, -0.000604967387311972, 0.0390343418636445, -0.0177526678478861, -0.0203680162281075, 0.0909707429432283, 0.0265593340493631, -0.11337542483867 });
        data.put(1995, new double[]{ 0.025384103494005, 0.375776696639684, 0.36793427543748, 0.0559544568178232, 0.144030311154288, 0.306137452881993, 0.110138130449177, 0.122372772922329, -0.0513545801425415 });
        data.put(1996, new double[]{ 0.0332247568261841, 0.229601750416008, 0.213535559764049, 0.0520707534655809, 0.039910711618534, 0.184384129528227, 0.0754970572924485, 0.370532319460791, 0.0932272040052513 });
        data.put(1997, new double[]{ 0.0170239604922995, 0.33363230400685, 0.313841581391171, 0.0525553960524572, 0.0769171263682569, 0.266688975205388, 0.0321735673447434, 0.196552919801834, -0.154072607917821 });
        data.put(1998, new double[]{ 0.0161190326732532, 0.28578748661778, 0.242990850446515, 0.0485592100040151, 0.0861569007542808, -0.0222419585912248, 0.198492637675068, -0.170074020848753, -0.205484847057372 });
        data.put(1999, new double[]{ 0.0268456374591295, 0.210415427501544, 0.252196703460881, 0.0468383548914517, 0.0040054201589752, 0.326141877863347, 0.330400729382663, -0.0258349748173913, 0.64668484771475 });
        data.put(2000, new double[]{ 0.0338680919654015, -0.0910438833122681, -0.114169402931887, 0.058932229245716, 0.102529136922394, -0.112415070935197, -0.137012612006041, 0.310440998253786, -0.320001041260158 });
        data.put(2001, new double[]{ 0.0155172411088402, -0.118858012557529, -0.111478601948434, 0.0382574432105574, 0.0815918444716786, 0.176031423619546, -0.210840095503847, 0.123487928080433, -0.0230816227756625 });
        data.put(2002, new double[]{ 0.0237691013637265, -0.221015657534177, -0.211482730910354, 0.0164667979498603, 0.0929071988040506, -0.19679033528742, -0.134791898408604, 0.0358217677095582, -0.0629936607878360 });
        data.put(2003, new double[]{ 0.0187949136328534, 0.286896287887334, 0.316223712914076, 0.0102147282903322, 0.0210003824909921, 0.587422200094905, 0.409854580420423, 0.361799855493620, 0.554706248935936 });
        data.put(2004, new double[]{ 0.0325556154035596, 0.108788010571925, 0.119718370038203, 0.0120257605678482, 0.0201993098707414, 0.196497827682721, 0.209621051800048, 0.331633657014545, 0.293946813215008 });
        data.put(2005, new double[]{ 0.0341565922828708, 0.0491277561058763, 0.0616416276380598, 0.029795334505178, 0.0155885414561925, 0.05698083591515, 0.149631163853247, 0.138196901837115, 0.360639501341382 });
        data.put(2006, new double[]{ 0.0254065056389576, 0.157964024344255, 0.154815662998423, 0.0479963133009762, 0.0351010582588682, 0.167934071840025, 0.258367715887374, 0.359744528318242, 0.335682717031705 });
        data.put(2007, new double[]{ 0.0408126866073373, 0.0549378904570788, 0.0581111048501515, 0.046622367908566, 0.0883316983136388, -0.0262364034543513, 0.12361089330447, -0.175549608936702, 0.404484371081372 });
        data.put(2008, new double[]{ 0.000914128169101858, -0.369970487746807, -0.367057339180168, 0.0159914263245082, 0.113505208634102, -0.387228110220984, -0.426595912435444, -0.392002106604394, -0.537657613572957 });
        data.put(2009, new double[]{ 0.0272133097848841, 0.264636514650552, 0.288201995675966, 0.000967612039104226, -0.0140986608339915, 0.474918147144904, 0.337134237781369, 0.284556036999120, 0.818673586403569 });
        data.put(2010, new double[]{ 0.0149572349281533, 0.150635969717307, 0.177339875393616, 0.00121376460958955, 0.0528947232133403, 0.301118443328488, 0.119155259882441, 0.280742076531905, 0.227362474977608 });
        data.put(2011, new double[]{ 0.0296241900299823, 0.0211201096242011, 0.00772495593004186, 0.000417370123839911, 0.0656963033233362, -0.0556600412125183, -0.125957318903428, 0.0936758587086617, -0.187396540450630 });
        data.put(2012, new double[]{ 0.0174102245821566, 0.1600350406695, 0.161618155254867, 0.000596859026924523, 0.0171403070527021, 0.179571516717369, 0.173773519886425, 0.171211746912286, 0.193284636030351 });
        data.put(2013, new double[]{ 0.0150173584098008, 0.323881275057416, 0.351695443904557, 0.00023792520243715, -0.0133951593397842, 0.443556663261882, 0.225841301715942, 0.0121579159315017, 0.000616150482380284 });
        data.put(2014, new double[]{ 0.00756493297817174, 0.136885318002251, 0.116460891628217, 0.000162911112166153, 0.0257446519470603, 0.040179443083425, -0.0396105673616749, 0.31998647540475, 9.6415133504335e-05 });
        data.put(2015, new double[]{ 0.00729519889507713, 0.0138383293712689, -0.00453833051792485, 0.000189010982487359, 0.0118280279450838, -0.0788284012775104, -0.00505683836404491, 0.0447993684292560, -0.111406849419144 });
        data.put(2016, new double[]{ 0.0207462217018184, 0.11959922130142, 0.135817182657445, 0.00199931119685837, 0.0105777379788028, 0.227011348266041, 0.0307160064048906, 0.0667662583205284, 0.104611711555105 });
        data.put(2017, new double[]{ 0.0210908248352095, 0.218315644920906, 0.210536585359634, 0.00797231778626006, 0.0113830177350271, 0.118809212142793, 0.267675266443838, 0.0376345143889036, 0.356674114576806 });
        data.put(2018, new double[]{ 0.0191015886783887, -0.0438434932252576, -0.0502212473006611, 0.0183052013153322, 0.014106852089026, -0.110513916904664, -0.137388408493307, -0.0421898066271309, -0.138996937708205 });
        data.put(2019, new double[]{ 0.0228512976038873, 0.314864201945832, 0.303703033499525, 0.0215330218405392, 0.0522382763756946, 0.252979657731764, 0.219798938752813, 0.231035273029540, 0.168739601994645 });
        data.put(2020, new double[]{ 0.0136200548537357, 0.183987792112461, 0.234520246054071, 0.00450588732120849, 0.0577488597318134, 0.28106232082806, 0.104026661682632, -0.111978565431785, 0.181683233776393 });
        data.put(2021, new double[]{ 0.0703640288761671, 0.287054168639597, 0.239712916701816, 0.000410274897509, -0.0171819388705249, 0.142283138844607, 0.124849399738195, 0.459131111775669, 0.0233896986393620 });
        data.put(2022, new double[]{ 0.0645440133153104, -0.181108651578205, -0.1976698589829, 0.0143412856760212, -0.0777326757890633, -0.235242641256725, -0.156806747996136, -0.25961301598719, -0.1776 });
        data.put(2023, new double[]{ 0.0335212284518966, 0.262876274954443, 0.266175744835143, 0.0494759937999094, 0.0428281707447658, 0.178406489080181, 0.1582, 0.1396, 0.1247 });
        data.put(2024, new double[]{ 0.0288805722207874, 0.250197204614459, 0.251513555682974, 0.0536572126829791, 0.0241962109944416, 0.161068183188536, 0.0389, 0.081, 0.0643 });
        data.put(2025, new double[]{ 0.0267708054525304, 0.178799926603734, 0.174686658132095, 0.0425475485920301, 0.0650591740262574, 0.117599783035545, 0.3217, 0.0367, 0.2957 });
        return Collections.unmodifiableMap(data);
    }

    /** Exposes valid year range to the controller for metadata endpoint */
    public int getMinYear() { return Collections.min(historicalData.keySet()); }
    public int getMaxYear() { return Collections.max(historicalData.keySet()); }

    // -------------------------------------------------------------------------
    // ANNUITY COMPARISON
    // Runs two all-scenarios simulations — one without annuity, one with — and
    // returns both for side-by-side display.
    // -------------------------------------------------------------------------

    public AnnuityCompareResponse simulateAllCompare(AnnuityCompareRequest req) {
        double annuityRate          = AnnuityRateTable.lookup(req.getAge(), req.isJoint());
        double annuityPurchaseAmt   = req.getStartingNestEgg() * req.getAnnuityPercentage();
        double initialAnnuityIncome = annuityPurchaseAmt * annuityRate;

        // Without annuity: standard run using full nest egg
        AllScenariosResponse withoutAnnuity = simulateAll(req.toAllScenariosRequest());

        // With annuity: portfolio starts smaller; annuity income offsets withdrawals
        AllScenariosResponse withAnnuity = simulateAllWithAnnuity(req, annuityRate);

        AnnuityCompareResponse resp = new AnnuityCompareResponse();
        resp.setWithoutAnnuity(withoutAnnuity);
        resp.setWithAnnuity(withAnnuity);
        resp.setAnnuityRate(annuityRate);
        resp.setInitialAnnuityIncome(initialAnnuityIncome);
        return resp;
    }

    private AllScenariosResponse simulateAllWithAnnuity(AnnuityCompareRequest req, double annuityRate) {
        int scenarioYears = req.getYearCount();

        // Build allocation request from the compare request fields
        SimulationRequest base = new SimulationRequest();
        base.setExpensesAndMgmtFee(req.getExpensesAndMgmtFee());
        base.setSp500(req.getSp500());
        base.setCrsp1_10(req.getCrsp1_10());
        base.setOneMonth(req.getOneMonth());
        base.setFiveYearUS(req.getFiveYearUS());
        base.setCrsp6_10(req.getCrsp6_10());
        base.setFfIntl(req.getFfIntl());
        base.setDjUsReit(req.getDjUsReit());
        base.setFfEmgMkts(req.getFfEmgMkts());

        // Annuity configuration
        double annuityPct           = req.getAnnuityPercentage();
        double portfolioNestEgg     = req.getStartingNestEgg() * (1.0 - annuityPct);
        double initialAnnuityIncome = req.getStartingNestEgg() * annuityPct * annuityRate;

        // Build per-scenario SimulationRequest with the reduced portfolio nest egg
        SimulationRequest portfolioReq = new SimulationRequest();
        portfolioReq.setStartingNestEgg(portfolioNestEgg);
        portfolioReq.setInitialWithdrawal(req.getInitialWithdrawal());
        portfolioReq.setExpensesAndMgmtFee(req.getExpensesAndMgmtFee());
        portfolioReq.setSp500(req.getSp500());
        portfolioReq.setCrsp1_10(req.getCrsp1_10());
        portfolioReq.setOneMonth(req.getOneMonth());
        portfolioReq.setFiveYearUS(req.getFiveYearUS());
        portfolioReq.setCrsp6_10(req.getCrsp6_10());
        portfolioReq.setFfIntl(req.getFfIntl());
        portfolioReq.setDjUsReit(req.getDjUsReit());
        portfolioReq.setFfEmgMkts(req.getFfEmgMkts());

        int maxYear = Collections.max(historicalData.keySet());
        int lastStartYear = maxYear - scenarioYears + 1;

        List<ScenarioSummary> scenarios = new ArrayList<>();
        int failureCount = 0;
        int earliestFailureYears = Integer.MAX_VALUE;
        double highestEndingBalance = 0;
        double totalEndingBalance = 0;
        int survivorCount = 0;
        int worstStartYear = FIRST_START_YEAR;
        int bestStartYear = FIRST_START_YEAR;
        double worstBalance = Double.MAX_VALUE;
        double bestBalance = Double.MIN_VALUE;

        for (int startYear = FIRST_START_YEAR; startYear <= lastStartYear; startYear++) {
            portfolioReq.setStartYear(startYear);
            List<YearResult> results = simulateWithAnnuity(portfolioReq, initialAnnuityIncome);

            List<YearResult> window = results.size() > scenarioYears
                ? results.subList(0, scenarioYears)
                : results;

            ScenarioSummary s = new ScenarioSummary();
            s.setStartYear(startYear);

            boolean failed = window.size() < scenarioYears
                || window.get(window.size() - 1).getPortfolioEnd() <= 0;
            s.setFailed(failed);
            s.setYearsSurvived(window.size());

            double endBalance = failed ? 0 : window.get(window.size() - 1).getPortfolioEnd();
            s.setEndingBalance(endBalance);
            scenarios.add(s);

            if (failed) {
                failureCount++;
                earliestFailureYears = Math.min(earliestFailureYears, window.size());
                if (endBalance < worstBalance) { worstBalance = endBalance; worstStartYear = startYear; }
            } else {
                totalEndingBalance += endBalance;
                survivorCount++;
                if (endBalance > highestEndingBalance) highestEndingBalance = endBalance;
                if (endBalance > bestBalance) { bestBalance = endBalance; bestStartYear = startYear; }
                if (endBalance < worstBalance) { worstBalance = endBalance; worstStartYear = startYear; }
            }
        }

        AllScenariosResponse resp = new AllScenariosResponse();
        resp.setScenarios(scenarios);
        resp.setTotalScenarios(scenarios.size());
        resp.setFailureCount(failureCount);
        resp.setFailureRate(Math.round((failureCount * 100.0 / scenarios.size()) * 10.0) / 10.0);
        resp.setEarliestFailureYears(earliestFailureYears == Integer.MAX_VALUE ? 0 : earliestFailureYears);
        resp.setHighestEndingBalance(highestEndingBalance);
        resp.setAverageEndingBalance(survivorCount > 0 ? totalEndingBalance / survivorCount : 0);
        resp.setWorstStartYear(worstStartYear);
        resp.setBestStartYear(bestStartYear);
        resp.setYearCount(scenarioYears);
        return resp;
    }

    /**
     * Simulates a single scenario with annuity income supplementing portfolio withdrawals.
     *
     * Each year:
     *   annuityIncome grows by min(CPI, 3%)
     *   portfolioWithdrawal = max(0, inflationAdjustedTarget - annuityIncome)
     *
     * @param req                 portfolio simulation parameters (nestEgg already reduced by annuityPct)
     * @param initialAnnuityIncome  annuity income in year 1
     */
    private List<YearResult> simulateWithAnnuity(SimulationRequest req, double initialAnnuityIncome) {
        List<YearResult> results = new ArrayList<>();
        int maxYear = Collections.max(historicalData.keySet());

        double annuityIncome = initialAnnuityIncome;
        double targetIncome  = req.getInitialWithdrawal(); // full inflation-adjusted income need

        for (int seq = 1; ; seq++) {
            int year = req.getStartYear() + (seq - 1);
            if (year > maxYear) break;

            double[] row = historicalData.get(year);
            if (row == null) break;

            YearResult r = new YearResult();
            r.setSequenceNumber(seq);
            r.setYear(year);
            r.setInflation(row[0]);

            double beginning;
            double portfolioWithdrawal;

            if (seq == 1) {
                beginning = req.getStartingNestEgg();
                portfolioWithdrawal = Math.max(0, targetIncome - annuityIncome);
                portfolioWithdrawal = Math.min(portfolioWithdrawal, beginning);
            } else {
                YearResult prev = results.get(seq - 2);
                beginning = prev.getPortfolioEnd();

                // Annuity grows by CPI, capped at 3%
                double cpi = prev.getInflation();
                annuityIncome = annuityIncome * (1.0 + Math.min(cpi, 0.03));

                // Full income target also grows by CPI
                targetIncome = targetIncome * (1.0 + cpi);

                portfolioWithdrawal = Math.max(0, targetIncome - annuityIncome);
                portfolioWithdrawal = Math.min(portfolioWithdrawal, Math.max(0, beginning));
            }

            r.setPortfolioBeginning(beginning);
            r.setAnnualWithdrawal(portfolioWithdrawal);

            double rate = computeBlendedReturn(year, req);
            r.setPortfolioReturnRate(rate);

            double gain = (beginning > 0)
                ? rate * (beginning - portfolioWithdrawal)
                : 0.0;
            r.setPortfolioReturnDollars(gain);
            r.setTotalIncome(portfolioWithdrawal + annuityIncome);
            r.setPortfolioEnd(beginning - portfolioWithdrawal + gain);

            results.add(r);

            if (r.getPortfolioEnd() <= 0) break;
        }

        return results;
    }
}
