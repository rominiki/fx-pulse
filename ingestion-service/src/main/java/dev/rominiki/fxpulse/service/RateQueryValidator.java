package dev.rominiki.fxpulse.service;

import java.time.LocalDate;

/** Common validation for the read-only ECB/EUR query endpoints. */
public final class RateQueryValidator {
    private RateQueryValidator() { }

    public static void validate(LocalDate from, LocalDate to, String quote) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        if (quote == null || !quote.matches("[A-Z]{3}") || "EUR".equals(quote)) {
            throw new IllegalArgumentException("quote must be a three-letter uppercase currency other than EUR");
        }
    }
}
