package com.portfolio.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.simulator.model.AllScenariosRequest;
import com.portfolio.simulator.model.AnnuityCompareRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the @JsonProperty fix on manual-allocation mXxx fields.
 *
 * Root cause: Jackson's Java Beans naming rules map getMCrsp1_10() to property
 * "MCrsp1_10" (both chars after stripping "get" are uppercase), so JSON keys
 * like "mCrsp1_10" (lowercase m) were silently dropped and all mXxx fields
 * stayed at 0. With manualAllocations=true and all allocations at 0, the
 * blended return was 0% every year — portfolios failed prematurely and the
 * Scattergram / Outcomes Grid showed incorrect exhaustion.
 *
 * Fix: added @JsonProperty("mXxx") to each mXxx field in AllScenariosRequest
 * and AnnuityCompareRequest, pinning the JSON key regardless of naming rules.
 */
class AllScenariosRequestDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // AllScenariosRequest
    // -------------------------------------------------------------------------

    @Test
    void allScenariosRequest_manualAllocations_deserializesCorrectly() throws Exception {
        String json = """
                {
                  "manualAllocations": true,
                  "mSp500": 0.10,
                  "mCrsp1_10": 0.40,
                  "mCrsp6_10": 0.05,
                  "mFfIntl": 0.10,
                  "mFfEmgMkts": 0.05,
                  "mDjUsReit": 0.05,
                  "mOneMonth": 0.05,
                  "mFiveYearUS": 0.20
                }
                """;

        AllScenariosRequest req = mapper.readValue(json, AllScenariosRequest.class);

        assertTrue(req.isManualAllocations(), "manualAllocations flag should be true");
        assertEquals(0.10, req.getMSp500(),     0.001, "mSp500 should deserialize to 0.10");
        assertEquals(0.40, req.getMCrsp1_10(),  0.001, "mCrsp1_10 should deserialize to 0.40");
        assertEquals(0.05, req.getMCrsp6_10(),  0.001, "mCrsp6_10 should deserialize to 0.05");
        assertEquals(0.10, req.getMFfIntl(),    0.001, "mFfIntl should deserialize to 0.10");
        assertEquals(0.05, req.getMFfEmgMkts(), 0.001, "mFfEmgMkts should deserialize to 0.05");
        assertEquals(0.05, req.getMDjUsReit(),  0.001, "mDjUsReit should deserialize to 0.05");
        assertEquals(0.05, req.getMOneMonth(),  0.001, "mOneMonth should deserialize to 0.05");
        assertEquals(0.20, req.getMFiveYearUS(),0.001, "mFiveYearUS should deserialize to 0.20");
    }

    @Test
    void allScenariosRequest_manualAllocations_derivedGettersReturnManualValues() throws Exception {
        String json = """
                {
                  "stockMarketAllocation": 0.60,
                  "manualAllocations": true,
                  "mSp500": 0.0,
                  "mCrsp1_10": 0.50,
                  "mCrsp6_10": 0.0,
                  "mFfIntl": 0.0,
                  "mFfEmgMkts": 0.0,
                  "mDjUsReit": 0.0,
                  "mOneMonth": 0.0,
                  "mFiveYearUS": 0.50
                }
                """;

        AllScenariosRequest req = mapper.readValue(json, AllScenariosRequest.class);

        // Derived getters must return manual values, not the auto-formula values
        assertEquals(0.50, req.getCrsp1_10(),  0.001,
                "getCrsp1_10() should return manual value 0.50, not auto-formula 0.60*0.56=0.336");
        assertEquals(0.50, req.getFiveYearUS(), 0.001,
                "getFiveYearUS() should return manual value 0.50, not auto-formula");
        assertEquals(0.0,  req.getCrsp6_10(),  0.001,
                "getCrsp6_10() should return manual value 0.0, not auto-formula 0.60*0.10=0.06");
    }

    // -------------------------------------------------------------------------
    // AnnuityCompareRequest (same @JsonProperty fix applied here too)
    // -------------------------------------------------------------------------

    @Test
    void annuityCompareRequest_manualAllocations_deserializesCorrectly() throws Exception {
        String json = """
                {
                  "manualAllocations": true,
                  "mSp500": 0.0,
                  "mCrsp1_10": 0.50,
                  "mCrsp6_10": 0.0,
                  "mFfIntl": 0.0,
                  "mFfEmgMkts": 0.0,
                  "mDjUsReit": 0.0,
                  "mOneMonth": 0.0,
                  "mFiveYearUS": 0.50
                }
                """;

        AnnuityCompareRequest req = mapper.readValue(json, AnnuityCompareRequest.class);

        assertTrue(req.isManualAllocations(), "manualAllocations flag should be true");
        assertEquals(0.50, req.getMCrsp1_10(),  0.001, "mCrsp1_10 should deserialize to 0.50");
        assertEquals(0.50, req.getMFiveYearUS(), 0.001, "mFiveYearUS should deserialize to 0.50");
        assertEquals(0.0,  req.getMSp500(),      0.001, "mSp500 should deserialize to 0.0");
    }
}
