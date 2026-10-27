package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.service.BackfillResult;
import dev.rominiki.fxpulse.service.BackfillService;
import dev.rominiki.fxpulse.service.FxRateIngestionService;
import dev.rominiki.fxpulse.service.IngestionResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {
    private final FxRateIngestionService ingestionService;
    private final BackfillService backfillService;

    public IngestionController(FxRateIngestionService ingestionService,
                               BackfillService backfillService) {
        this.ingestionService = ingestionService;
        this.backfillService = backfillService;
    }

    @PostMapping
    public IngestionResult ingestDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "quote", defaultValue = "USD") String quote) {
        return ingestionService.ingest(date, date, quote);
    }

    @PostMapping("/backfill")
    public BackfillResult backfill(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "quote", defaultValue = "USD") String quote) {
        return backfillService.backfill(from, to, quote);
    }
}
