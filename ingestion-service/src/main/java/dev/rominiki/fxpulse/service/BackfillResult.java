package dev.rominiki.fxpulse.service;

import java.time.LocalDate;
import java.util.List;

public record BackfillResult(
        LocalDate from,
        LocalDate to,
        String quote,
        int received,
        int inserted,
        int skippedDuplicates,
        int ignoredOutsideRange,
        List<BackfillWindowResult> windows
) {}
