# Portfolio Simulator — Backend

## Related Projects
- **Frontend:** `~/Desktop/Code/PortfolioSimulator/portfolio-frontend-vite`
- The frontend's API contract is defined in `src/types/index.ts` in the frontend repo.

## Cross-Repo Rules
When making backend changes, check the frontend if any of the following are affected:

| Backend change | Frontend impact |
|---|---|
| Endpoint path changes (`/api/simulate`, `/api/simulate/all`, `/api/simulate/metadata`, `/api/simulate/defaults`) | `src/hooks/useSimulator.ts` |
| Field added/removed/renamed on `SimulationRequest` | `src/types/index.ts`, `src/hooks/useSimulator.ts`, `App.tsx` |
| Field added/removed/renamed on `SimulationResponse` or `YearResult` | `src/types/index.ts`, `ResultsTable.tsx`, `PortfolioChart.tsx`, `SummaryCards.tsx` |
| Field added/removed/renamed on `AllScenariosResponse` or `ScenarioSummary` | `src/types/index.ts`, `StatCards.tsx`, `OutcomesChart.tsx`, `OutcomesHeatmap.tsx`, `SorrExplainer.tsx` |
| Default allocation or year range changes | `src/hooks/useSimulator.ts` (defaults endpoint consumer) |
| Error response shape changes | All hooks in `src/hooks/useSimulator.ts` |

## Architecture
- Java 21 + Spring Boot 3.2 REST API
- Core logic in `SimulatorService.java`
- Historical data (1929–2025) stored in `HISTORICAL_DATA` map — 9 values per year: `{ CPI, SP500, CRSP1_10, T-Bills, 5YrTreas, CRSP6_10, FFIntl, REIT, EmgMkts }`
- Blended return formula: each asset class allocation × its own actual historical return, minus fee
