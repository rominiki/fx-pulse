package dev.rominiki.fxpulse.service;

public record IngestionResult(
        int received,
        int inserted,
        int skippedDuplicates,
        int ignoredOutsideRange
) {}
