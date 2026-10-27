package dev.rominiki.fxpulse.client;

import dev.rominiki.fxpulse.client.dto.FrankfurterRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FrankfurterClientTest {
    private static final String URL = "https://api.frankfurter.dev/v2/providers/ecb/rates"
            + "?from=2025-01-02&to=2025-01-03&base=EUR&quotes=USD";
    private MockRestServiceServer server;
    private FrankfurterClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FrankfurterClient(builder,
                "https://api.frankfurter.dev/v2/providers/ecb/rates");
    }

    private List<FrankfurterRateResponse> fetch() {
        return client.fetchRates(LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3), "EUR", "USD");
    }

    @Test
    void fetchRates_shouldReturnRates_whenApiResponseIsSuccessful() {
        String json = """
                [{"date":"2025-01-02","base":"EUR","quote":"USD","rate":1.0289},
                 {"date":"2025-01-03","base":"EUR","quote":"USD","rate":1.0300}]
                """;
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        List<FrankfurterRateResponse> rates = fetch();
        assertThat(rates).hasSize(2);
        assertThat(rates.getFirst().date()).isEqualTo(LocalDate.of(2025, 1, 2));
        assertThat(rates.getFirst().base()).isEqualTo("EUR");
        assertThat(rates.getFirst().quote()).isEqualTo("USD");
        assertThat(rates.getFirst().rate()).isEqualByComparingTo("1.0289");
        assertThat(rates.get(1).rate()).isEqualByComparingTo("1.0300");
        server.verify();
    }

    @Test
    void fetchRates_shouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> client.fetchRates(LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 1, 2), "EUR", "USD"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Start date");
        server.verify();
    }

    @Test
    void fetchRates_shouldRejectBlankCurrencies() {
        assertThatThrownBy(() -> client.fetchRates(LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3), "EUR", ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Currencies");
        server.verify();
    }

    @Test
    void fetchRates_shouldReturnEmptyList_whenApiReturnsEmptyArray() {
        server.expect(requestTo(URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(fetch()).isEmpty();
        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenResponseBodyIsMissing() {
        server.expect(requestTo(URL)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThatThrownBy(this::fetch).isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("empty response body");
        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenApiReturns422() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(this::fetch).isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("422");
        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenApiReturns503() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertThatThrownBy(this::fetch).isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("503");
        server.verify();
    }

    @Test
    void fetchRates_shouldThrow_whenJsonIsMalformed() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{ invalid json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(this::fetch).isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("Failed to process");
        server.verify();
    }
}
