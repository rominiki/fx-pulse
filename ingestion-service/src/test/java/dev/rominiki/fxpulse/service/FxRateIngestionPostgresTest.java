package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.client.FrankfurterClient;
import dev.rominiki.fxpulse.client.dto.FrankfurterRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
class FxRateIngestionPostgresTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FxRateIngestionService ingestion;
    @Autowired private BackfillService backfill;
    @MockitoBean private FrankfurterClient client;

    private static final LocalDate JAN2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate JAN3 = LocalDate.of(2025, 1, 3);

    private FrankfurterRateResponse row(LocalDate date, String value) {
        return new FrankfurterRateResponse(date, "EUR", "USD", new BigDecimal(value));
    }

    private int countRates() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM raw.fx_rates", Integer.class);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE TABLE raw.fx_rates");
        reset(client);
    }

    @Test
    void rerunningTheSamePayloadDoesNotAddRows() {
        when(client.fetchRates(JAN2, JAN3, "EUR", "USD"))
                .thenReturn(List.of(row(JAN2, "1.0289"), row(JAN3, "1.0300")));
        IngestionResult first = ingestion.ingest(JAN2, JAN3, "USD");
        IngestionResult second = ingestion.ingest(JAN2, JAN3, "USD");
        assertThat(first.inserted()).isEqualTo(2);
        assertThat(second.inserted()).isZero();
        assertThat(second.skippedDuplicates()).isEqualTo(2);
        assertThat(countRates()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT rate FROM raw.fx_rates WHERE rate_date = ?",
                BigDecimal.class, JAN2)).isEqualByComparingTo("1.0289");
        assertThat(jdbc.queryForObject("SELECT ingested_at IS NOT NULL FROM raw.fx_rates LIMIT 1",
                Boolean.class)).isTrue();
    }

    @Test
    void conflictingSecondRecordRollsBackFirstInsertInBatch() {
        when(client.fetchRates(JAN3, JAN3, "EUR", "USD"))
                .thenReturn(List.of(row(JAN3, "1.0500")));
        ingestion.ingest(JAN3, JAN3, "USD");
        when(client.fetchRates(JAN2, JAN3, "EUR", "USD"))
                .thenReturn(List.of(row(JAN2, "1.0289"), row(JAN3, "1.9999")));
        assertThatThrownBy(() -> ingestion.ingest(JAN2, JAN3, "USD"))
                .isInstanceOf(FxRateConflictException.class);
        assertThat(countRates()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT rate FROM raw.fx_rates WHERE rate_date = ?",
                BigDecimal.class, JAN3)).isEqualByComparingTo("1.0500");
    }

    @Test
    void invalidSourceBatchNeverWritesAnyRows() {
        when(client.fetchRates(JAN2, JAN3, "EUR", "USD"))
                .thenReturn(List.of(row(JAN2, "1.0289"), row(JAN3, "-1")));
        assertThatThrownBy(() -> ingestion.ingest(JAN2, JAN3, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(countRates()).isZero();
    }

    @Test
    void failedMonthCanBeResumedWithoutDuplicates() {
        LocalDate jan31 = LocalDate.of(2025, 1, 31);
        LocalDate feb1 = LocalDate.of(2025, 2, 1);
        LocalDate feb3 = LocalDate.of(2025, 2, 3);
        when(client.fetchRates(JAN2, jan31, "EUR", "USD"))
                .thenReturn(List.of(row(JAN2, "1.0289")));
        when(client.fetchRates(feb1, feb3, "EUR", "USD"))
                .thenThrow(new IllegalStateException("simulated outage"))
                .thenReturn(List.of(row(feb3, "1.0300")));
        assertThatThrownBy(() -> backfill.backfill(JAN2, feb3, "USD"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(countRates()).isEqualTo(1);
        BackfillResult resumed = backfill.backfill(JAN2, feb3, "USD");
        assertThat(resumed.inserted()).isEqualTo(1);
        assertThat(resumed.skippedDuplicates()).isEqualTo(1);
        assertThat(countRates()).isEqualTo(2);
    }
}
