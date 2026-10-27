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

    private final RestClient restClient;

    public FrankfurterClient(
            RestClient.Builder builder,
            @Value("${fxpulse.api.base-url}") String baseUrl
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public List<FrankfurterRateResponse> fetchRates(
            LocalDate from,
            LocalDate to,
            String base,
            String quote
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        if (base == null || base.isBlank()
                || quote == null || quote.isBlank()) {
            throw new IllegalArgumentException(
                    "Currencies cannot be blank"
            );
        }

        FrankfurterRateResponse[] response;

        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("base", base)
                            .queryParam("quotes", quote)
                            .build())
                    .retrieve()
                    .body(FrankfurterRateResponse[].class);

        } catch (RestClientResponseException e) {
            throw new FrankfurterClientException(
                    "Frankfurter API returned HTTP "
                            + e.getStatusCode().value(),
                    e
            );

        } catch (ResourceAccessException e) {
            throw new FrankfurterClientException(
                    "Cannot connect to Frankfurter API",
                    e
            );

        } catch (RestClientException e) {
            throw new FrankfurterClientException(
                    "Failed to process Frankfurter API response",
                    e
            );
        }

        if (response == null) {
            throw new FrankfurterClientException(
                    "Frankfurter returned an empty response body"
            );
        }

        return List.of(response);
    }
}
