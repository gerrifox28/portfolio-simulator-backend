package com.portfolio.simulator;

import com.portfolio.simulator.service.SpreadsheetLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpreadsheetLoaderServiceTest {

    private SpreadsheetLoaderService loader;

    @BeforeEach
    void setUp() {
        loader = new SpreadsheetLoaderService();
    }

    private InputStream openTestFile() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test-returns.xltm");
        assertNotNull(is, "test-returns.xltm not found in test resources");
        return is;
    }

    @Test
    void load_parsesExpectedYearCount() throws IOException {
        Map<Integer, double[]> data = loader.load(openTestFile());
        // Spreadsheet covers 1929–2025 = 97 years
        assertEquals(97, data.size(), "Expected 97 years of data (1929–2025)");
    }

    @Test
    void load_containsFirstAndLastYear() throws IOException {
        Map<Integer, double[]> data = loader.load(openTestFile());
        assertTrue(data.containsKey(1929), "Should contain 1929 (first year)");
        assertTrue(data.containsKey(2025), "Should contain 2025 (last year)");
    }

    @Test
    void load_eachRowHasNineValues() throws IOException {
        Map<Integer, double[]> data = loader.load(openTestFile());
        data.forEach((year, values) ->
            assertEquals(9, values.length,
                "Year " + year + " should have 9 values (CPI, SP500, CRSP1-10, T-Bills, 5YrTreas, CRSP6-10, FFIntl, REIT, EmgMkts)"));
    }

    @Test
    void load_1929_cpiIsNegative() throws IOException {
        // 1929 had deflation (-0.58%)
        Map<Integer, double[]> data = loader.load(openTestFile());
        double cpi = data.get(1929)[0];
        assertTrue(cpi < 0, "1929 CPI should be negative (deflation year)");
    }

    @Test
    void load_1933_sp500IsLargePositive() throws IOException {
        // 1933 S&P 500 returned ~+54% — market recovery after Depression bottom
        Map<Integer, double[]> data = loader.load(openTestFile());
        double sp500 = data.get(1933)[1];
        assertTrue(sp500 > 0.50, "1933 S&P 500 should be above 50% (was ~54%)");
    }

    @Test
    void load_2008_sp500IsLargeNegative() throws IOException {
        // 2008 financial crisis: S&P 500 fell ~-37%
        Map<Integer, double[]> data = loader.load(openTestFile());
        double sp500 = data.get(2008)[1];
        assertTrue(sp500 < -0.30, "2008 S&P 500 should be below -30% (financial crisis)");
    }

    @Test
    void load_1986_ffIntlDifferentFromCrsp110() throws IOException {
        // From 1975+ FFIntl has real data — in 1986 it was ~+66.9%, very different from CRSP 1-10 ~+16%
        Map<Integer, double[]> data = loader.load(openTestFile());
        double crsp1_10 = data.get(1986)[2]; // index 2
        double ffIntl   = data.get(1986)[6]; // index 6
        assertNotEquals(crsp1_10, ffIntl, 0.01,
            "By 1986 F/F International should have its own real return data, different from CRSP 1-10");
        assertTrue(ffIntl > 0.5, "1986 F/F International should be above 50% (~66.9%)");
    }

    @Test
    void load_badStream_throwsIOException() {
        byte[] garbage = "not an excel file".getBytes();
        assertThrows(Exception.class, () ->
            loader.load(new java.io.ByteArrayInputStream(garbage)),
            "Should throw when given invalid file bytes");
    }
}
