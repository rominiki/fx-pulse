package dev.rominiki.fxpulse.persistence;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class FxRatePagePostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired FxRateRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetRows() {
        jdbc.execute("TRUNCATE raw.fx_rates");
    }

    private void insert(LocalDate date, String quote, String provider, String value) {
        repository.saveAndFlush(new FxRate(new FxRateId(provider, date, "EUR", quote),
                new BigDecimal(value)));
    }

    @Test
    void paginationFiltersProviderCurrencyAndDateThenSorts() {
        LocalDate d2 = LocalDate.of(2025, 1, 2);
        LocalDate d3 = LocalDate.of(2025, 1, 3);
        LocalDate d6 = LocalDate.of(2025, 1, 6);
        insert(d6, "USD", "ECB", "1.0600");
        insert(d2, "USD", "ECB", "1.0200");
        insert(d3, "USD", "ECB", "1.0300");
        insert(d3, "GBP", "ECB", "0.8300");
        insert(d3, "USD", "OTHER", "1.9000");

        Page<FxRate> first = repository.findPageByCurrencyAndDateRange(
                "ECB", "EUR", "USD", d2, d6, PageRequest.of(0, 2));
        Page<FxRate> second = repository.findPageByCurrencyAndDateRange(
                "ECB", "EUR", "USD", d2, d6, PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(r -> r.getId().getRateDate())
                .containsExactly(d2, d3);
        assertThat(second.getContent()).extracting(r -> r.getId().getRateDate())
                .containsExactly(d6);
    }
}
