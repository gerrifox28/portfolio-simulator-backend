# Portfolio Simulator

Retirement portfolio simulator translated from `Template_for_Gerri_March_2026.xltm`.

Simulates how a portfolio performs over historical market cycles given a starting
nest egg, initial withdrawal, and custom asset allocation.

## Stack

- **Backend**: Java 21 + Spring Boot 3.2
- **Frontend**: React *(coming soon)*

## Running locally

```bash
./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`.

## API Endpoints

### `GET /api/simulate/defaults`
Returns the default input values (mirrors spreadsheet defaults).

### `GET /api/simulate/metadata`
Returns available year range and asset class labels — useful for driving the React form.

### `POST /api/simulate`
Runs the simulation. Returns per-year results plus summary stats.

**Request body:**
```json
{
  "startYear": 1970,
  "startingNestEgg": 1000000,
  "initialWithdrawal": 40000,
  "expensesAndMgmtFee": 0.012,
  "sp500": 0.0,
  "crsp1_10": 0.3111,
  "oneMonth": 0.05,
  "fiveYearUS": 0.25,
  "crsp6_10": 0.0549,
  "ffIntl": 0.162,
  "djUsReit": 0.10,
  "ffEmgMkts": 0.072
}
```
> Note: allocation weights must sum to 1.0

**Response:**
```json
{
  "inputs": { ... },
  "yearlyResults": [
    {
      "sequenceNumber": 1,
      "year": 1970,
      "inflation": 0.0557,
      "portfolioBeginning": 1000000.0,
      "annualWithdrawal": 40000.0,
      "portfolioReturnRate": 0.0412,
      "portfolioReturnDollars": 39552.0,
      "totalIncome": 40000.0,
      "portfolioEnd": 999552.0
    },
    ...
  ],
  "yearsSurvived": 56,
  "portfolioExhausted": false,
  "finalPortfolioValue": 4823910.22,
  "finalYear": 2025
}
```

## Running tests

```bash
./mvnw test
```

## Project structure

```
src/
├── main/java/com/portfolio/simulator/
│   ├── SimulatorApplication.java       # Spring Boot entry point
│   ├── controller/
│   │   └── SimulatorController.java    # REST endpoints
│   ├── service/
│   │   └── SimulatorService.java       # Core simulation logic + historical data
│   └── model/
│       ├── SimulationRequest.java      # Input model (Inputs and Chart sheet)
│       ├── SimulationResponse.java     # Output wrapper with summary stats
│       └── YearResult.java             # One row of output (Calcs and Formulas sheet)
└── test/java/com/portfolio/simulator/
    └── SimulatorServiceTest.java       # Unit tests verified against spreadsheet
```
