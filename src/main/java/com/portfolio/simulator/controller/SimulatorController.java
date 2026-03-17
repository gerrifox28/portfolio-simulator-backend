package com.portfolio.simulator.controller;

import com.portfolio.simulator.model.AllScenariosRequest;
import com.portfolio.simulator.model.AllScenariosResponse;
import com.portfolio.simulator.model.SimulationRequest;
import com.portfolio.simulator.model.SimulationResponse;
import com.portfolio.simulator.model.YearResult;
import com.portfolio.simulator.service.SimulatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
    "http://localhost:3000",                          // local dev
    "https://${FRONTEND_URL:localhost:3000}"          // Vercel — set FRONTEND_URL env var on Render
})
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    /**
     * POST /api/simulate
     *
     * Runs the portfolio simulation with the provided inputs.
     *
     * Example request body:
     * {
     *   "startYear": 1970,
     *   "startingNestEgg": 1000000,
     *   "initialWithdrawal": 40000,
     *   "expensesAndMgmtFee": 0.012,
     *   "sp500": 0.0,
     *   "crsp1_10": 0.3111,
     *   "oneMonth": 0.05,
     *   "fiveYearUS": 0.25,
     *   "crsp6_10": 0.0549,
     *   "ffIntl": 0.162,
     *   "djUsReit": 0.10,
     *   "ffEmgMkts": 0.072
     * }
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@Valid @RequestBody SimulationRequest request) {
        // Validate allocation weights sum to ~1.0 (within floating-point tolerance)
        double sum = request.allocationSum();
        if (Math.abs(sum - 1.0) > 0.001) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", String.format(
                    "Asset allocation weights must sum to 1.0 (got %.4f). " +
                    "Adjust your weights so sp500 + crsp1_10 + oneMonth + fiveYearUS + " +
                    "crsp6_10 + ffIntl + djUsReit + ffEmgMkts = 1.0", sum)
            ));
        }

        List<YearResult> results = simulatorService.simulate(request);
        SimulationResponse response = SimulationResponse.of(request, results);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/simulate/defaults
     *
     * Returns the default input values (mirrors spreadsheet defaults).
     * Useful for pre-populating the React form.
     */
    @GetMapping("/simulate/defaults")
    public ResponseEntity<SimulationRequest> getDefaults() {
        SimulationRequest defaults = new SimulationRequest();
        return ResponseEntity.ok(defaults);
    }

    /**
     * GET /api/simulate/metadata
     *
     * Returns the available year range and asset class descriptions.
     * Useful for driving form validation and labels in the React UI.
     */
    @GetMapping("/simulate/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata() {
        return ResponseEntity.ok(Map.of(
            "minYear", simulatorService.getMinYear(),
            "maxYear", simulatorService.getMaxYear(),
            "assetClasses", List.of(
                Map.of("key", "sp500",      "label", "S&P 500"),
                Map.of("key", "crsp1_10",   "label", "CRSP 1-10 (Total US Market)"),
                Map.of("key", "oneMonth",   "label", "One-Month T-Bills"),
                Map.of("key", "fiveYearUS", "label", "5-Year US Treasuries"),
                Map.of("key", "crsp6_10",   "label", "CRSP 6-10 (Small Cap)"),
                Map.of("key", "ffIntl",     "label", "F/F International"),
                Map.of("key", "djUsReit",   "label", "DJ US REIT"),
                Map.of("key", "ffEmgMkts",  "label", "F/F Emerging Markets")
            )
        ));
    }
    /**
     * POST /api/simulate/all
     *
     * Runs all 40-year historical scenarios (1929-1986) and returns
     * aggregate statistics for the SORR visualization.
     */
    @PostMapping("/simulate/all")
    public ResponseEntity<?> simulateAll(@Valid @RequestBody AllScenariosRequest request) {
        AllScenariosResponse response = simulatorService.simulateAll(request);
        return ResponseEntity.ok(response);
    }
}