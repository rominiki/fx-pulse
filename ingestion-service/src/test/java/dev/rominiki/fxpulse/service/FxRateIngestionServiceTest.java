package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.client.FrankfurterClient;
import dev.rominiki.fxpulse.client.dto.FrankfurterRateResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/fxpulse_migration_test"
})
class FxRateIngestionServiceTest {

    private static final LocalDate FROM = LocalDate.of(2025, 2, 4);
    private static final LocalDate TO = LocalDate.of(2025, 2, 5);

    @MockitoBean
    private FrankfurterClient client;

    @Autowired
    private FxRateIngestionService ingestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void removeTestRows() {
        jdbcTemplate.update("""
                DELETE FROM raw.fx_rates
                WHERE provider = 'ECB' AND rate_date BETWEEN ? AND ?
                  AND base_currency = 'EUR' AND quote_currency = 'USD'
                """, FROM, TO);
    }

    private FrankfurterRateResponse row(LocalDate date, String rate) {
        return new FrankfurterRateResponse(date, "EUR", "USD", new BigDecimal(rate));
    }

    private int storedRows() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM raw.fx_rates
                WHERE provider = 'ECB' AND rate_date BETWEEN ? AND ?
                  AND base_currency = 'EUR' AND quote_currency = 'USD'
                """, Integer.class, FROM, TO);
        return count;
    }

    @Test
    void sameBatchTwiceInsertsOnlyOnce() {
        when(client.fetchRates(FROM, TO, "EUR", "USD"))
                .thenReturn(List.of(row(FROM, "1.0289"), row(TO, "1.0300")));

        IngestionResult first = ingestionService.ingest(FROM, TO, "USD");
        IngestionResult second = ingestionService.ingest(FROM, TO, "USD");

        assertThat(first.inserted()).isEqualTo(2);
        assertThat(first.skippedDuplicates()).isZero();
        assertThat(second.inserted()).isZero();
        assertThat(second.skippedDuplicates()).isEqualTo(2);
        assertThat(storedRows()).isEqualTo(2);
    }

    @Test
    void changedRateIsRejectedWithoutOverwritingExistingValue() {
        when(client.fetchRates(FROM, TO, "EUR", "USD"))
                .thenReturn(List.of(row(TO, "1.0300")),
                        List.of(row(TO, "1.9999")));

        ingestionService.ingest(FROM, TO, "USD");

        assertThatThrownBy(() -> ingestionService.ingest(FROM, TO, "USD"))
                .isInstanceOf(FxRateConflictException.class);
        assertThat(storedRows()).isEqualTo(1);
        BigDecimal stored = jdbcTemplate.queryForObject("""
                SELECT rate FROM raw.fx_rates
                WHERE provider = 'ECB' AND rate_date = ?
                  AND base_currency = 'EUR' AND quote_currency = 'USD'
                """, BigDecimal.class, TO);
        assertThat(stored).isEqualByComparingTo("1.0300");
    }

    @Test
    void invalidSourceRateDoesNotWriteRows() {
        when(client.fetchRates(FROM, TO, "EUR", "USD"))
                .thenReturn(List.of(row(FROM, "-1.00")));

        assertThatThrownBy(() -> ingestionService.ingest(FROM, TO, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storedRows()).isZero();
    }

    @Test
    void conflictRollsBackEarlierInsertsFromSameBatch() {
        when(client.fetchRates(FROM, TO, "EUR", "USD"))
                .thenReturn(List.of(row(TO, "1.0300")),
                        List.of(row(FROM, "1.0289"), row(TO, "1.9999")));

        ingestionService.ingest(FROM, TO, "USD");

        assertThatThrownBy(() -> ingestionService.ingest(FROM, TO, "USD"))
                .isInstanceOf(FxRateConflictException.class);
        assertThat(storedRows()).isEqualTo(1);
    }

    @Test
    void sourceDatesOutsideRequestedRangeAreIgnored() {
        when(client.fetchRates(FROM, TO, "EUR", "USD"))
                .thenReturn(List.of(row(FROM.minusDays(1), "1.0289")));

        IngestionResult result = ingestionService.ingest(FROM, TO, "USD");

        assertThat(result.received()).isEqualTo(1);
        assertThat(result.ignoredOutsideRange()).isEqualTo(1);
        assertThat(result.inserted()).isZero();
        assertThat(storedRows()).isZero();
    }
}
