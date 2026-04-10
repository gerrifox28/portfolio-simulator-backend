package com.portfolio.simulator.controller;

import com.portfolio.simulator.service.SimulatorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "https://${FRONTEND_URL:localhost:3000}"
})
public class DataController {

    private final SimulatorService simulatorService;

    public DataController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    /**
     * POST /api/data/upload
     *
     * Accepts a .xlsx or .xltm spreadsheet, parses the "Advanced Returns" sheet,
     * replaces the in-memory historical data, and saves the file to disk so it
     * survives a server restart.
     *
     * Returns a summary of the years loaded.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided."));
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xltm") && !filename.endsWith(".xls")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Unsupported file type. Please upload an Excel file (.xlsx or .xltm)."));
        }

        try {
            byte[] bytes = file.getBytes();
            int yearsLoaded = simulatorService.loadFromSpreadsheet(new ByteArrayInputStream(bytes), bytes);

            return ResponseEntity.ok(Map.of(
                "message", "Historical data updated successfully.",
                "yearsLoaded", yearsLoaded,
                "minYear", simulatorService.getMinYear(),
                "maxYear", simulatorService.getMaxYear()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to parse spreadsheet: " + e.getMessage()));
        }
    }
}
