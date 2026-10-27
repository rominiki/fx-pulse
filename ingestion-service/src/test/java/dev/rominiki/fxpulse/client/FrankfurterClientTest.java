package dev.rominiki.fxpulse.client;

import dev.rominiki.fxpulse.client.dto.FrankfurterRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class FrankfurterClientTest {

    private FrankfurterClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        server = MockRestServiceServer.bindTo(builder).build();

        client = new FrankfurterClient(
                builder,
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
        );
    }

    @Test
    void fetchRates_shouldReturnRates_whenApiResponseIsSuccessful() {

        String json = """
                [
                  {
                    "date": "2025-01-02",
                    "base": "EUR",
                    "quote": "USD",
                    "rate": 1.0289
                  },
                  {
                    "date": "2025-01-03",
                    "base": "EUR",
                    "quote": "USD",
                    "rate": 1.0300
                  }
                ]
                """;

        String expectedUrl =
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
                        + "?from=2025-01-02"
                        + "&to=2025-01-03"
                        + "&base=EUR"
                        + "&quotes=USD";

        server.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<FrankfurterRateResponse> rates = client.fetchRates(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                "EUR",
                "USD"
        );

        assertThat(rates).hasSize(2);

        FrankfurterRateResponse firstRate = rates.get(0);

        assertThat(firstRate.date())
                .isEqualTo(LocalDate.of(2025, 1, 2));

        assertThat(firstRate.base())
                .isEqualTo("EUR");

        assertThat(firstRate.quote())
                .isEqualTo("USD");

        assertThat(firstRate.rate())
                .isEqualByComparingTo("1.0289");

        FrankfurterRateResponse secondRate = rates.get(1);

        assertThat(secondRate.date())
                .isEqualTo(LocalDate.of(2025, 1, 3));

        assertThat(secondRate.rate())
                .isEqualByComparingTo("1.0300");

        server.verify();
    }


    @Test
    void fetchRates_shouldRejectInvalidDateRange() {

        assertThatThrownBy(() -> client.fetchRates(
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 1, 2),
                "EUR",
                "USD"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date");

        server.verify();
    }

    @Test
    void fetchRates_shouldRejectBlankCurrencies() {

        assertThatThrownBy(() -> client.fetchRates(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                "EUR",
                ""
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currencies");

        server.verify();
    }

    @Test
    void fetchRates_shouldReturnEmptyList_whenApiReturnsEmptyArray() {

        String expectedUrl =
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
                        + "?from=2025-01-04"
                        + "&to=2025-01-05"
                        + "&base=EUR"
                        + "&quotes=USD";

        server.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[]",
                        MediaType.APPLICATION_JSON
                ));

        List<FrankfurterRateResponse> rates = client.fetchRates(
                LocalDate.of(2025, 1, 4),
                LocalDate.of(2025, 1, 5),
                "EUR",
                "USD"
        );

        assertThat(rates).isEmpty();

        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenResponseBodyIsMissing() {

        String expectedUrl =
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
                        + "?from=2025-01-02"
                        + "&to=2025-01-03"
                        + "&base=EUR"
                        + "&quotes=USD";

        server.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(
                        "",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.fetchRates(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                "EUR",
                "USD"
        ))
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("empty response body");

        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenApiReturns422() {

        String expectedUrl =
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
                        + "?from=2025-01-02"
                        + "&to=2025-01-03"
                        + "&base=EUR"
                        + "&quotes=USD";

        server.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatusCode.valueOf(422)));

        assertThatThrownBy(() -> client.fetchRates(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                "EUR",
                "USD"
        ))
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("422");

        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenApiReturns503() {

        String expectedUrl =
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
                        + "?from=2025-01-02"
                        + "&to=2025-01-03"
                        + "&base=EUR"
                        + "&quotes=USD";

        server.expect(requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.fetchRates(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                "EUR",
                "USD"
        ))
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("503");

        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenJsonIsMalformed() {

        String expectedUrl =
                "https://api.frankfurter.dev/v2/providers/ecb/rates"
                        + "?from=2025-01-02"
                        + "&to=2025-01-03"
                        + "&base=EUR"
                        + "&quotes=USD";

        server.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(
                        "{ invalid json",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.fetchRates(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3),
                "EUR",
                "USD"
        ))
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("Failed to process");

        server.verify();
    }
}
