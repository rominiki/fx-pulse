package dev.rominiki.fxpulse.service;

import java.time.LocalDate;

public record BackfillWindowResult(
        LocalDate from,
        LocalDate to,
        IngestionResult result
) {}
