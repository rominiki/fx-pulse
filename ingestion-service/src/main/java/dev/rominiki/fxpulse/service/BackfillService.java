package dev.rominiki.fxpulse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BackfillService {
    private static final Logger log = LoggerFactory.getLogger(BackfillService.class);
    private static final long MAX_DAYS = 366;
    private final FxRateIngestionService ingestionService;

    public BackfillService(FxRateIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public BackfillResult backfill(LocalDate from, LocalDate to, String quote) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid backfill date range");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new IllegalArgumentException("Backfill range must be at most 366 days");
        }
        if (quote == null || !quote.matches("[A-Z]{3}") || "EUR".equals(quote)) {
            throw new IllegalArgumentException("Quote must be a three-letter code other than EUR");
        }

        List<BackfillWindowResult> windows = new ArrayList<>();
        int received = 0, inserted = 0, skipped = 0, ignored = 0;
        LocalDate cursor = from;

        // Each month is persisted in its own transaction by FxRateWriteService.
        // A failed month stops this run; rerunning safely skips committed months.
        while (!cursor.isAfter(to)) {
            LocalDate end = YearMonth.from(cursor).atEndOfMonth();
            if (end.isAfter(to)) end = to;
            log.info("Starting ECB backfill window {}..{} quote={}", cursor, end, quote);
            IngestionResult result = ingestionService.ingest(cursor, end, quote);
            windows.add(new BackfillWindowResult(cursor, end, result));
            received += result.received();
            inserted += result.inserted();
            skipped += result.skippedDuplicates();
            ignored += result.ignoredOutsideRange();
            log.info("Completed ECB backfill window {}..{} received={} inserted={} skipped={} ignored={}",
                    cursor, end, result.received(), result.inserted(),
                    result.skippedDuplicates(), result.ignoredOutsideRange());
            cursor = end.plusDays(1);
        }
        return new BackfillResult(from, to, quote, received, inserted, skipped, ignored,
                List.copyOf(windows));
    }
}
