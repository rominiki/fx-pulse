package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.client.FrankfurterClient;
import dev.rominiki.fxpulse.client.dto.FrankfurterRateResponse;
import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FxRateIngestionService {

    private final FrankfurterClient client;
    private final FxRateWriteService writeService;

    public FxRateIngestionService(FrankfurterClient client, FxRateWriteService writeService) {
        this.client = client;
        this.writeService = writeService;
    }

    public IngestionResult ingest(LocalDate from, LocalDate to, String quote) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid ingestion date range");
        }
        if (quote == null || !quote.matches("[A-Z]{3}") || quote.equals("EUR")) {
            throw new IllegalArgumentException("Quote must be a three-letter code other than EUR");
        }

        List<FrankfurterRateResponse> response = client.fetchRates(from, to, "EUR", quote);
        if (response == null) {
            throw new IllegalStateException("Frankfurter client returned null list");
        }

        List<FxRate> validRates = new ArrayList<>();
        int ignored = 0;

        for (FrankfurterRateResponse row : response) {
            if (row == null || row.date() == null
                    || !"EUR".equals(row.base()) || !quote.equals(row.quote())
                    || row.rate() == null || row.rate().signum() <= 0) {
                throw new IllegalArgumentException("Invalid Frankfurter exchange-rate record: " + row);
            }

            BigDecimal normalizedRate;
            try {
                normalizedRate = row.rate().setScale(10, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Rate has more than 10 significant decimal places", e);
            }
            if (normalizedRate.precision() > 20) {
                throw new IllegalArgumentException("Rate exceeds NUMERIC(20,10) precision");
            }

            // The source may report a publication date outside a requested range.
            // Do not persist that unrelated observation or invent calendar-day rates.
            if (row.date().isBefore(from) || row.date().isAfter(to)) {
                ignored++;
                continue;
            }

            FxRateId id = new FxRateId("ECB", row.date(), "EUR", quote);
            validRates.add(new FxRate(id, normalizedRate));
        }

        FxRateWriteService.WriteCounts counts = writeService.persist(validRates);
        return new IngestionResult(response.size(), counts.inserted(), counts.skipped(), ignored);
    }
}
