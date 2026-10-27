package dev.rominiki.fxpulse.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FrankfurterClientResilienceTest {
    private final String url = "https://api.frankfurter.dev/v2/providers/ecb/rates"
            + "?from=2025-01-02&to=2025-01-03&base=EUR&quotes=USD";
    private FrankfurterClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FrankfurterClient(builder,
                "https://api.frankfurter.dev/v2/providers/ecb/rates");
    }

    private void fetch() {
        client.fetchRates(LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 3), "EUR", "USD");
    }

    @Test
    void neverRetriesClientValidationError() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(this::fetch)
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("422");
        server.verify();
    }

    @Test
    void retriesTransientFailureAndEventuallySucceeds() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(url)).andRespond(withSuccess("""
                [{"date":"2025-01-02","base":"EUR","quote":"USD","rate":1.0289}]
                """, MediaType.APPLICATION_JSON));
        assertThat(client.fetchRates(
                LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 3), "EUR", "USD"))
                .hasSize(1);
        server.verify();
    }

    @Test
    void stopsAfterThreeTransientFailures() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertThatThrownBy(this::fetch)
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("after 3 attempt(s)");
        server.verify();
    }

    @Test
    void retriesRateLimitResponse() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(url)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(client.fetchRates(
                LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 3), "EUR", "USD"))
                .isEmpty();
        server.verify();
    }

    @Test
    void malformedJsonFailsWithoutRetry() {
        server.expect(requestTo(url)).andRespond(withSuccess("{ broken json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(this::fetch)
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("Failed to process");
        server.verify();
    }

    @Test
    void missingBodyFailsWithoutRetry() {
        server.expect(requestTo(url)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThatThrownBy(this::fetch)
                .isInstanceOf(FrankfurterClientException.class)
                .hasMessageContaining("empty response body");
        server.verify();
    }
}
