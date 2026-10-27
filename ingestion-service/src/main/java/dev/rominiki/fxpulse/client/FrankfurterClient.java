package dev.rominiki.fxpulse.client;

import dev.rominiki.fxpulse.client.dto.FrankfurterRateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.List;

@Component
public class FrankfurterClient {
    private static final int MAX_ATTEMPTS = 3;
    private final RestClient restClient;

    public FrankfurterClient(RestClient.Builder builder,
                             @Value("${fxpulse.api.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<FrankfurterRateResponse> fetchRates(
            LocalDate from, LocalDate to, String base, String quote) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (base == null || base.isBlank() || quote == null || quote.isBlank()) {
            throw new IllegalArgumentException("Currencies cannot be blank");
        }
        if (!base.matches("[A-Z]{3}") || !quote.matches("[A-Z]{3}") || base.equals(quote)) {
            throw new IllegalArgumentException("Invalid currency pair");
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                FrankfurterRateResponse[] response = restClient.get()
                        .uri(builder -> builder
                                .queryParam("from", from)
                                .queryParam("to", to)
                                .queryParam("base", base)
                                .queryParam("quotes", quote)
                                .build())
                        .retrieve()
                        .body(FrankfurterRateResponse[].class);
                if (response == null) {
                    throw new FrankfurterClientException("Frankfurter returned an empty response body");
                }
                return List.of(response);

            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean transientFailure = status == 429 || status >= 500;
                if (!transientFailure || attempt == MAX_ATTEMPTS) {
                    throw new FrankfurterClientException(
                            "Frankfurter API returned HTTP " + status + " after " + attempt + " attempt(s)",
                            e, transientFailure);
                }

            } catch (ResourceAccessException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new FrankfurterClientException(
                            "Frankfurter API connection failed after " + MAX_ATTEMPTS + " attempts",
                            e, true);
                }

            } catch (RestClientException e) {
                throw new FrankfurterClientException("Failed to process Frankfurter API response", e);
            }
            pauseBeforeRetry(attempt);
        }
        throw new IllegalStateException("Unreachable retry state");
    }

    private static void pauseBeforeRetry(int failedAttempt) {
        try {
            Thread.sleep(100L * failedAttempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrankfurterClientException("Interrupted while retrying Frankfurter request", e, true);
        }
    }
}
