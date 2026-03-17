package com.portfolio.simulator.service;

import com.portfolio.simulator.model.AllScenariosRequest;
import com.portfolio.simulator.model.AllScenariosResponse;
import com.portfolio.simulator.model.ScenarioSummary;
import com.portfolio.simulator.model.SimulationRequest;
import com.portfolio.simulator.model.YearResult;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Core simulation engine.
 *
 * Translates the logic from "Calcs and Formulas" and "Advanced Returns"
 * sheets in Template_for_Gerri_March_2026.xltm.
 */
@Service
public class SimulatorService {

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    public List<YearResult> simulate(SimulationRequest req) {
        List<YearResult> results = new ArrayList<>();
        int maxYear = Collections.max(HISTORICAL_DATA.keySet());

        for (int seq = 1; ; seq++) {
            int year = req.getStartYear() + (seq - 1);
            if (year > maxYear) break;

            double[] row = HISTORICAL_DATA.get(year);
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
    // ALL SCENARIOS  (runs all 40-year windows from 1929 to 1986)
    // -------------------------------------------------------------------------

    private static final int SCENARIO_YEARS = 40;
    private static final int FIRST_START_YEAR = 1929;

    public AllScenariosResponse simulateAll(AllScenariosRequest req) {
        // Build a default SimulationRequest using spreadsheet allocation defaults
        SimulationRequest base = defaultAllocation();
        base.setStartingNestEgg(req.getStartingNestEgg());
        base.setInitialWithdrawal(req.getInitialWithdrawal());

        int maxYear = Collections.max(HISTORICAL_DATA.keySet());
        int lastStartYear = maxYear - SCENARIO_YEARS + 1; // last year with full 40yr data

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

            // Cap at 40 years
            List<YearResult> window = results.size() > SCENARIO_YEARS
                ? results.subList(0, SCENARIO_YEARS)
                : results;

            ScenarioSummary s = new ScenarioSummary();
            s.setStartYear(startYear);

            boolean failed = results.size() < SCENARIO_YEARS
                || results.get(results.size() - 1).getPortfolioEnd() <= 0;
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
        resp.setEarliestFailureYear(earliestFailureYears == Integer.MAX_VALUE ? 0 : earliestFailureYears);
        resp.setHighestFinalBalance(highestEndingBalance);
        resp.setAverageFinalBalance(survivorCount > 0 ? totalEndingBalance / survivorCount : 0);
        resp.setWorstStartYear(worstStartYear);
        resp.setBestStartYear(bestStartYear);
        return resp;
    }

    /** Spreadsheet default allocation */
    private SimulationRequest defaultAllocation() {
        SimulationRequest r = new SimulationRequest();
        r.setSp500(0.0);
        r.setCrsp1_10(0.31110);
        r.setOneMonth(0.05);
        r.setFiveYearUS(0.25);
        r.setCrsp6_10(0.0549);
        r.setFfIntl(0.162);
        r.setDjUsReit(0.10);
        r.setFfEmgMkts(0.072);
        r.setExpensesAndMgmtFee(0.012);
        return r;
    }

    // -------------------------------------------------------------------------
    // BLENDED RETURN
    // Mirrors column K formula in "Advanced Returns":
    //   K = (sp500 × C)
    //     + ((crsp1_10 + ffIntl + djUsReit + ffEmgMkts) × D)
    //     + (oneMonth × E)
    //     + (fiveYearUS × F)
    //     + (crsp6_10 × G)
    //     - expensesAndMgmtFee
    // -------------------------------------------------------------------------
    private double computeBlendedReturn(int year, SimulationRequest req) {
        double[] row = HISTORICAL_DATA.get(year);
        if (row == null) return -req.getExpensesAndMgmtFee();

        double sp500    = row[1];
        double crsp1_10 = row[2];
        double tBills   = row[3];
        double treas5yr = row[4];
        double crsp6_10 = row[5];

        // F/F Intl, DJ US REIT, F/F Emg Mkts all use CRSP 1-10 returns
        // as their proxy (matching the spreadsheet grouping in the K formula)
        double broadEquity = req.getCrsp1_10() + req.getFfIntl()
                           + req.getDjUsReit() + req.getFfEmgMkts();

        return (req.getSp500()      * sp500)
             + (broadEquity         * crsp1_10)
             + (req.getOneMonth()   * tBills)
             + (req.getFiveYearUS() * treas5yr)
             + (req.getCrsp6_10()   * crsp6_10)
             - req.getExpensesAndMgmtFee();
    }

    // -------------------------------------------------------------------------
    // HISTORICAL DATA  (from "Advanced Returns" sheet, columns A–G)
    // Key   = year
    // Value = double[] { CPI, SP500, CRSP1_10, T-Bills(1mo), 5YrTreasuries, CRSP6_10 }
    // -------------------------------------------------------------------------
    private static final Map<Integer, double[]> HISTORICAL_DATA;
    static {
        Map<Integer, double[]> data = new HashMap<>();
        data.put(1926, new double[]{ -0.011173184590433, 0.116074444282409, 0.0838984830783489, 0.0326645200508318, 0.0537701100062276, -0.0107904334894637 });
        data.put(1927, new double[]{ -0.0225988700829455, 0.374799109880638, 0.334826238298318, 0.0312472050940384, 0.0452359037079946, 0.282100205416146 });
        data.put(1928, new double[]{ -0.0115606938424807, 0.436052079229754, 0.383851257975714, 0.035578972466231, 0.00920846770922012, 0.337601591725529 });
        data.put(1929, new double[]{ 0.0058479530597344, -0.0840910453519469, -0.3168000000001, 0.047480176730095, 0.06013819159676, -0.404410654413495 });
        data.put(1930, new double[]{ -0.0639534872551056, -0.248950061276622, -0.287990281699459, 0.0241034069838444, 0.067151105314349, -0.399295399634056 });
        data.put(1931, new double[]{ -0.0931677017713394, -0.433485834389562, -0.435242334097107, 0.0107302656053732, -0.0232016139970406, -0.501937518941534 });
        data.put(1932, new double[]{ -0.102739726098865, -0.0819889433459114, -0.0862439407785889, 0.00961727928086287, 0.0881120571705954, -0.0219058898301721 });
        data.put(1933, new double[]{ 0.00763358734609909, 0.539700780152496, 0.566514038249488, 0.00297458494476444, 0.0182708997212342, 1.24665139345477 });
        data.put(1934, new double[]{ 0.015151516049303, -0.0143073320033779, 0.0408923626333539, 0.00162813092957403, 0.0899635023733998, 0.221080465438252 });
        data.put(1935, new double[]{ 0.0298507456072412, 0.47656357234639, 0.444313103395225, 0.00168629565580947, 0.0700661825303834, 0.591905085144078 });
        data.put(1936, new double[]{ 0.0144927539454549, 0.339224020976951, 0.323182484635028, 0.00177441615910023, 0.0305697783361889, 0.563082507230172 });
        data.put(1937, new double[]{ 0.0285714289621817, -0.350227601202219, -0.347362671872794, 0.00307920207578105, 0.015581317292904, -0.490546747864878 });
        data.put(1938, new double[]{ -0.027777778830841, 0.311368112790911, 0.281629905480163, -0.000162215781152497, 0.0623030475364112, 0.404051662763712 });
        data.put(1939, new double[]{ -1.10162634570088e-09, -0.0042237308645644, 0.0282556984709668, 0.000204994109742618, 0.0452438524431764, 0.0372196374444105 });
        data.put(1940, new double[]{ 0.0071428569732801, -0.0977883629758554, -0.0709463243204035, 4.79789782639184e-05, 0.0296141256064264, -0.0593063701582961 });
        data.put(1941, new double[]{ 0.0992907804568053, -0.115766722348485, -0.100780292232219, 0.000597109930394302, 0.00495020808437063, -0.104176858947881 });
        data.put(1942, new double[]{ 0.0903225806656451, 0.203329775128291, 0.161224960199017, 0.00268024766937658, 0.0193632382825721, 0.284977943863029 });
        data.put(1943, new double[]{ 0.0295857984396677, 0.259075922153561, 0.284267189871869, 0.00347150958948239, 0.0281003668220834, 0.636430013345024 });
        data.put(1944, new double[]{ 0.0229885059027848, 0.197299025726789, 0.214907383492044, 0.0033049925731794, 0.0179523619557436, 0.452643019952508 });
        data.put(1945, new double[]{ 0.0224719103542372, 0.364122472215439, 0.384956337448742, 0.00329596336239946, 0.0221894308585813, 0.673251597843468 });
        data.put(1946, new double[]{ 0.181318682159817, -0.0807432501793661, -0.0617405233424988, 0.00353069966921771, 0.0100453335843578, -0.113265818041504 });
        data.put(1947, new double[]{ 0.0883720929073302, 0.0569655815016505, 0.0358488941190382, 0.00503437192809253, 0.00910961759414186, -0.0305597856168388 });
        data.put(1948, new double[]{ 0.0299145311675617, 0.0551082249529329, 0.0210895574090804, 0.00811175407460141, 0.0184762636032856, -0.0458690611469902 });
        data.put(1949, new double[]{ -0.0207468872006695, 0.187862140008587, 0.20217960398737, 0.0110263197845424, 0.0232310302416394, 0.213156156208004 });
        data.put(1950, new double[]{ 0.059322032928727, 0.317408529778294, 0.296104961028788, 0.0119559737907442, 0.00701235498243591, 0.382112294105359 });
        data.put(1951, new double[]{ 0.0600000019815019, 0.240159419064956, 0.20681888890524, 0.0149280626788948, 0.00361983757340956, 0.144073973313171 });
        data.put(1952, new double[]{ 0.00754716965116953, 0.18351009400481, 0.134165604655541, 0.0165601266983797, 0.0163317382958543, 0.0909311530115771 });
        data.put(1953, new double[]{ 0.00749063610154899, -0.00975132996013883, 0.006714837935901, 0.0182371852832297, 0.0323236356937355, -0.0341959886537327 });
        data.put(1954, new double[]{ -0.00743494399878719, 0.526222003007204, 0.499777345013846, 0.00863883465737603, 0.0268241133092539, 0.587144684260463 });
        data.put(1955, new double[]{ 0.00374531783626408, 0.315391373140896, 0.252141678834142, 0.015736551358245, -0.0065048765842497, 0.2100893638461 });
        data.put(1956, new double[]{ 0.0298507459779411, 0.0655537453592012, 0.0826477450600504, 0.0245924537771571, -0.00421089806401409, 0.0603162664431851 });
        data.put(1957, new double[]{ 0.0289855066298947, -0.107914266401983, -0.100469022990861, 0.0313905390802318, 0.0783852835501633, -0.173862227229936 });
        data.put(1958, new double[]{ 0.0176056345727504, 0.433716443081126, 0.450219917966576, 0.0154147139647338, -0.0128849734156832, 0.633303628473494 });
        data.put(1959, new double[]{ 0.0173010389136377, 0.119768462381524, 0.126686827044764, 0.0295196627982097, -0.00390635835106634, 0.174971795559865 });
        data.put(1960, new double[]{ 0.0136054426290018, 0.00464298138397168, 0.011584243024978, 0.0266273892290787, 0.117561462866872, -0.0363929067559212 });
        data.put(1961, new double[]{ 0.00671140973266593, 0.268861031225499, 0.269457975115085, 0.0212652628000469, 0.0184925752027389, 0.29719374014582 });
        data.put(1962, new double[]{ 0.0133333325448486, -0.0872777292139638, -0.101754861880897, 0.0273363345128017, 0.0556410874682753, -0.173694071555572 });
        data.put(1963, new double[]{ 0.0164473691282136, 0.227756746064169, 0.209779634208491, 0.0311651992766673, 0.0164070809795358, 0.165573238435953 });
        data.put(1964, new double[]{ 0.00970873700115771, 0.165077291370179, 0.161259333314641, 0.0353571900764573, 0.0404336630034508, 0.17095976786232 });
        data.put(1965, new double[]{ 0.0192307689961404, 0.124523160023627, 0.144630822186298, 0.0392723505369161, 0.0101842780670636, 0.360139931757527 });
        data.put(1966, new double[]{ 0.034591195111199, -0.100478095861859, -0.0874026515241073, 0.0475931167271655, 0.0468808555783076, -0.0746642533199042 });
        data.put(1967, new double[]{ 0.0303951368280846, 0.239871105754691, 0.287380379414093, 0.0421000772196323, 0.0100876589378578, 0.763574172642738 });
        data.put(1968, new double[]{ 0.0471976413637301, 0.110814211072406, 0.141415239945543, 0.0520579363250742, 0.0453501584645282, 0.379404675594467 });
        data.put(1969, new double[]{ 0.0619718307390991, -0.0848615153403149, -0.109136342929965, 0.0658380718408564, -0.00736647095543741, -0.257882653664172 });
        data.put(1970, new double[]{ 0.0557029174890977, 0.0402601462724581, 2.30808872938582e-05, 0.0652497050362031, 0.16858845416212, -0.122118950422231 });
        data.put(1971, new double[]{ 0.0326633166104562, 0.143179458097906, 0.161458204932623, 0.0438612806370726, 0.0872070471647404, 0.194807588857274 });
        data.put(1972, new double[]{ 0.0340632612901097, 0.189759135626222, 0.168371784419832, 0.0383995263059369, 0.0515956397655866, 0.0325947399845636 });
        data.put(1973, new double[]{ 0.0870588234111724, -0.146659442289406, -0.18064487899065, 0.069303142605702, 0.0460599921048492, -0.364691266422588 });
        data.put(1974, new double[]{ 0.123376622489003, -0.264582756753376, -0.270373038403411, 0.0800305884202763, 0.0568972352782073, -0.261340186984807 });
        data.put(1975, new double[]{ 0.0693641616850349, 0.37211960760346, 0.387533639326003, 0.0580359466802527, 0.0783118376776966, 0.639210339591356 });
        data.put(1976, new double[]{ 0.0486486500221821, 0.238489371146422, 0.267612232226168, 0.0508251603349714, 0.128696498062716, 0.514662066522774 });
        data.put(1977, new double[]{ 0.0670103094496774, -0.0717930128844977, -0.0425961120408163, 0.0512021148962267, 0.0140617271653316, 0.183839781211706 });
        data.put(1978, new double[]{ 0.0901771342634503, 0.0657395947838273, 0.074865063121758, 0.0718082326871996, 0.0348668443634375, 0.183334306141868 });
        data.put(1979, new double[]{ 0.132939436623745, 0.184239764922035, 0.226242878727847, 0.103762301903503, 0.0409384253965215, 0.455907106973593 });
        data.put(1980, new double[]{ 0.125162971648185, 0.324076851501436, 0.328143362798177, 0.112355116955396, 0.0390932530979178, 0.334591232654648 });
        data.put(1981, new double[]{ 0.0892236400825219, -0.049088144783707, -0.0364839543669018, 0.147088625966817, 0.094547100683396, 0.0422293639092492 });
        data.put(1982, new double[]{ 0.03829787095734, 0.214094274938087, 0.210005550880346, 0.105430854865416, 0.290965425592456, 0.288399734657231 });
        data.put(1983, new double[]{ 0.0379098366487054, 0.225138135911181, 0.219754452603502, 0.0879832226305428, 0.0740623191637044, 0.299479155591498 });
        data.put(1984, new double[]{ 0.0394866726255714, 0.062663224413356, 0.0451102837722452, 0.0984938733704892, 0.140183687945132, -0.0556280208435542 });
        data.put(1985, new double[]{ 0.0379867050632505, 0.321706866518365, 0.321681998492923, 0.0772329519628136, 0.203312895783437, 0.3134208029607 });
        data.put(1986, new double[]{ 0.0109789573413721, 0.184705266701603, 0.161904921199843, 0.0616243430970076, 0.151393061323308, 0.0710045572750093 });
        data.put(1987, new double[]{ 0.0443438927222688, 0.0523075800748809, 0.0167012168271128, 0.0546559566650471, 0.0290395621956887, -0.0909862134993626 });
        data.put(1988, new double[]{ 0.0441941071219638, 0.168092926389822, 0.1802791196717, 0.0634780614161086, 0.0610201235465189, 0.239108684907263 });
        data.put(1989, new double[]{ 0.0464730281465993, 0.314908447912292, 0.28863511170143, 0.0837042615754458, 0.132874914654257, 0.160925856702053 });
        data.put(1990, new double[]{ 0.06106265016586, -0.0310375912638435, -0.0595848076704465, 0.0781346982163191, 0.0973007426430561, -0.202285591911274 });
        data.put(1991, new double[]{ 0.0306427508122256, 0.304653911105353, 0.346669400697819, 0.0559527028200806, 0.154620862446788, 0.488744514311581 });
        data.put(1992, new double[]{ 0.0290065257589534, 0.076247831874581, 0.0979741321082779, 0.0350630703122445, 0.0719021463152068, 0.195206117518097 });
        data.put(1993, new double[]{ 0.0274841427157018, 0.100722516124989, 0.111415475893184, 0.0289694601209223, 0.112397284529764, 0.186882276151699 });
        data.put(1994, new double[]{ 0.0267489730367003, 0.0132012395753931, -0.000604967387311972, 0.0390343418636445, -0.0514382218846309, -0.0203680162281075 });
        data.put(1995, new double[]{ 0.025384103494005, 0.375776696639684, 0.36793427543748, 0.0559544568178232, 0.168015162855022, 0.306137452881993 });
        data.put(1996, new double[]{ 0.0332247568261841, 0.229601750416008, 0.213535559764049, 0.0520707534655809, 0.0209920776188568, 0.184384129528227 });
        data.put(1997, new double[]{ 0.0170239604922995, 0.33363230400685, 0.313841581391171, 0.0525553960524572, 0.0838110953420279, 0.266688975205388 });
        data.put(1998, new double[]{ 0.0161190326732532, 0.28578748661778, 0.242990850446515, 0.0485592100040151, 0.102053590252432, -0.0222419585912248 });
        data.put(1999, new double[]{ 0.0268456374591295, 0.210415427501544, 0.252196703460881, 0.0468383548914517, -0.0177065531802872, 0.326141877863347 });
        data.put(2000, new double[]{ 0.0338680919654015, -0.0910438833122681, -0.114169402931887, 0.058932229245716, 0.125921604121491, -0.112415070935197 });
        data.put(2001, new double[]{ 0.0155172411088402, -0.118858012557529, -0.111478601948434, 0.0382574432105574, 0.0761921632090434, 0.176031423619546 });
        data.put(2002, new double[]{ 0.0237691013637265, -0.221015657534177, -0.211482730910354, 0.0164667979498603, 0.129335253009753, -0.19679033528742 });
        data.put(2003, new double[]{ 0.0187949136328534, 0.286896287887334, 0.316223712914076, 0.0102147282903322, 0.0239580186985184, 0.587422200094905 });
        data.put(2004, new double[]{ 0.0325556154035596, 0.108788010571925, 0.119718370038203, 0.0120257605678482, 0.0225343627343764, 0.196497827682721 });
        data.put(2005, new double[]{ 0.0341565922828708, 0.0491277561058763, 0.0616416276380598, 0.029795334505178, 0.0136191886343655, 0.05698083591515 });
        data.put(2006, new double[]{ 0.0254065056389576, 0.157964024344255, 0.154815662998423, 0.0479963133009762, 0.0314282733836186, 0.167934071840025 });
        data.put(2007, new double[]{ 0.0408126866073373, 0.0549378904570788, 0.0581111048501515, 0.046622367908566, 0.10052723055318, -0.0262364034543513 });
        data.put(2008, new double[]{ 0.000914128169101858, -0.369970487746807, -0.367057339180168, 0.0159914263245082, 0.131066277084833, -0.387228110220984 });
        data.put(2009, new double[]{ 0.0272133097848841, 0.264636514650552, 0.288201995675966, 0.000967612039104226, -0.0240369863162039, 0.474918147144904 });
        data.put(2010, new double[]{ 0.0149572349281533, 0.150635969717307, 0.177339875393616, 0.00121376460958955, 0.071171715316882, 0.301118443328488 });
        data.put(2011, new double[]{ 0.0296241900299823, 0.0211201096242011, 0.00772495593004186, 0.000417370123839911, 0.0881082533391939, -0.0556600412125183 });
        data.put(2012, new double[]{ 0.0174102245821566, 0.1600350406695, 0.161618155254867, 0.000596859026924523, 0.0238462175207859, 0.179571516717369 });
        data.put(2013, new double[]{ 0.0150173584098008, 0.323881275057416, 0.351695443904557, 0.00023792520243715, -0.0201469515472142, 0.443556663261882 });
        data.put(2014, new double[]{ 0.00756493297817174, 0.136885318002251, 0.116460891628217, 0.000162911112166153, 0.0299609440051831, 0.040179443083425 });
        data.put(2015, new double[]{ 0.00729519889507713, 0.0138383293712689, -0.00453833051792485, 0.000189010982487359, 0.0179163100738342, -0.0788284012775104 });
        data.put(2016, new double[]{ 0.0207462217018184, 0.11959922130142, 0.135817182657445, 0.00199931119685837, 0.0192275773389263, 0.227011348266041 });
        data.put(2017, new double[]{ 0.0210908248352095, 0.218315644920906, 0.210536585359634, 0.00797231778626006, 0.016352196814881, 0.118809212142793 });
        data.put(2018, new double[]{ 0.0191015886783887, -0.0438434932252576, -0.0502212473006611, 0.0183052013153322, 0.0115991013656835, -0.110513916904664 });
        data.put(2019, new double[]{ 0.0228512976038873, 0.314864201945832, 0.303703033499525, 0.0215330218405392, 0.0666750437439292, 0.252979657731764 });
        data.put(2020, new double[]{ 0.0136200548537357, 0.183987792112461, 0.234520246054071, 0.00450588732120849, 0.0725331257430433, 0.28106232082806 });
        data.put(2021, new double[]{ 0.0703640288761671, 0.287054168639597, 0.239712916701816, 0.000410274897509, -0.0335379102343689, 0.142283138844607 });
        data.put(2022, new double[]{ 0.0645, -0.18109999999999998, -0.1977, 0.0143, -0.0935622294915766, -0.235242641256725 });
        data.put(2023, new double[]{ 0.0335, 0.26289999999999997, 0.2662, 0.0495, 0.028999999999999998, 0.1784 });
        data.put(2024, new double[]{ 0.0275, 0.2502, 0.2515, 0.0537, 0.033, 0.1611 });
        data.put(2025, new double[]{ 0.0268, 0.1788, 0.1747, 0.044, 0.0574, 0.1176 });
        HISTORICAL_DATA = Collections.unmodifiableMap(data);
    }

    /** Exposes valid year range to the controller for metadata endpoint */
    public int getMinYear() { return Collections.min(HISTORICAL_DATA.keySet()); }
    public int getMaxYear() { return Collections.max(HISTORICAL_DATA.keySet()); }
}
