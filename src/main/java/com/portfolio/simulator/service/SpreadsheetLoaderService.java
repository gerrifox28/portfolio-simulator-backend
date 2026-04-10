package com.portfolio.simulator.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses historical return data from the "Advanced Returns" sheet of the
 * master spreadsheet (.xlsx or .xltm).
 *
 * Expected sheet layout (columns A–J, rows 3+ are data):
 *   A: Year
 *   B: CPI / Inflation
 *   C: S&P 500
 *   D: CRSP 1-10 (Total Market)
 *   E: One-Month T-Bills
 *   F: 5-Year US Treasuries
 *   G: CRSP 6-10 (Small Cap)
 *   H: F/F International
 *   I: DJ US REIT
 *   J: F/F Emerging Markets
 *   K: Blended Return (ignored — calculated by the simulator)
 */
@Service
public class SpreadsheetLoaderService {

    private static final String SHEET_NAME = "Advanced Returns";
    private static final int HEADER_ROWS = 2;   // rows 1–2 are headers
    private static final int YEAR_COL    = 0;   // column A
    private static final int FIRST_DATA_COL = 1; // column B (CPI)
    private static final int LAST_DATA_COL  = 9; // column J (EmgMkts)
    private static final int DATA_COLS = LAST_DATA_COL - FIRST_DATA_COL + 1; // 9 values

    static {
        // Suppress POI's noisy byte-limit warning for large files
        IOUtils.setByteArrayMaxOverride(100_000_000);
    }

    /**
     * Parses the spreadsheet from the given input stream.
     *
     * @param inputStream raw bytes of the .xlsx or .xltm file
     * @return map of year → double[9] { CPI, SP500, CRSP1_10, T-Bills, 5YrTreas, CRSP6_10, FFIntl, REIT, EmgMkts }
     * @throws IOException if the file cannot be read or the expected sheet is missing
     */
    public Map<Integer, double[]> load(InputStream inputStream) throws IOException {
        Map<Integer, double[]> data = new LinkedHashMap<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IOException(
                    "Sheet \"" + SHEET_NAME + "\" not found. " +
                    "Please upload the correct master returns spreadsheet.");
            }

            for (int rowIdx = HEADER_ROWS; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                Cell yearCell = row.getCell(YEAR_COL);
                if (yearCell == null || yearCell.getCellType() == CellType.BLANK) continue;

                int year = (int) getNumeric(yearCell);
                if (year < 1900 || year > 2100) continue; // skip non-year rows

                double[] values = new double[DATA_COLS];
                for (int col = FIRST_DATA_COL; col <= LAST_DATA_COL; col++) {
                    values[col - FIRST_DATA_COL] = getNumeric(row.getCell(col));
                }
                data.put(year, values);
            }
        }

        if (data.isEmpty()) {
            throw new IOException("No data rows found in \"" + SHEET_NAME + "\" sheet.");
        }

        return data;
    }

    private double getNumeric(Cell cell) {
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case FORMULA -> cell.getNumericCellValue();
            default -> 0.0;
        };
    }
}
