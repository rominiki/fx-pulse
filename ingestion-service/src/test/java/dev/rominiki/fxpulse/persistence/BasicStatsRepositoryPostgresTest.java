package dev.rominiki.fxpulse.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class BasicStatsRepositoryPostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired BasicStatsRepository repository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE raw.fx_rates");
    }

    @Test
    void computesMinMaxMeanAndLatestOnlyForMatchingRows() {
        jdbc.update("""
                INSERT INTO raw.fx_rates (provider, rate_date, base_currency, quote_currency, rate)
                VALUES ('ECB', ?, 'EUR', 'USD', 1.0200),
                       ('ECB', ?, 'EUR', 'USD', 1.0300),
                       ('ECB', ?, 'EUR', 'USD', 1.0600),
                       ('ECB', ?, 'EUR', 'GBP', 0.8300),
                       ('OTHER', ?, 'EUR', 'USD', 1.9000)
                """, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 3),
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 3),
                LocalDate.of(2025, 1, 3));

        var stats = repository.summarize(LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31), "USD");
        assertThat(stats.observations()).isEqualTo(3);
        assertThat(stats.firstDate()).isEqualTo(LocalDate.of(2025, 1, 2));
        assertThat(stats.lastDate()).isEqualTo(LocalDate.of(2025, 1, 6));
        assertThat(stats.minRate()).isEqualByComparingTo("1.0200");
        assertThat(stats.maxRate()).isEqualByComparingTo("1.0600");
        assertThat(stats.averageRate()).isEqualByComparingTo("1.03666666666666666667");
        assertThat(stats.latestRate()).isEqualByComparingTo("1.0600");
    }

    @Test
    void emptyDateRangeHasZeroCountAndNullRates() {
        var stats = repository.summarize(LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 2, 28), "USD");
        assertThat(stats.observations()).isZero();
        assertThat(stats.minRate()).isNull();
        assertThat(stats.firstDate()).isNull();
        assertThat(stats.latestRate()).isNull();
    }
}
