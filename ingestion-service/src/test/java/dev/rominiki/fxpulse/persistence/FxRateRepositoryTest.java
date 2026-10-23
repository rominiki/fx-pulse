package dev.rominiki.fxpulse.persistence;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import dev.rominiki.fxpulse.web.dto.FxRateDto;
import dev.rominiki.fxpulse.web.dto.FxRateMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=" +
                "jdbc:postgresql://localhost:5432/fxpulse_migration_test"
})
class FxRateRepositoryTest {

    @Autowired
    private FxRateRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsExchangeRateWithoutLosingPrecision() {

        FxRateId id = new FxRateId(
                "ECB",
                LocalDate.of(2025, 1, 3),
                "EUR",
                "USD"
        );

        BigDecimal expectedRate =
                new BigDecimal("1.0299000000");

        FxRate entity = new FxRate(id, expectedRate);
        repository.saveAndFlush(entity);

        entityManager.clear();

        FxRate loaded = repository.findById(id)
                .orElseThrow();

        assertEquals(
                LocalDate.of(2025, 1, 3),
                loaded.getId().getRateDate()
        );

        assertEquals(
                "USD",
                loaded.getId().getQuoteCurrency()
        );

        assertEquals(
                0,
                expectedRate.compareTo(loaded.getRate())
        );

        assertNotNull(loaded.getIngestedAt());

        List<FxRate> results =
                repository.findByIdRateDateBetweenOrderByIdRateDateAsc(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31)
                );

        assertTrue(
                results.stream().anyMatch(
                        rate -> rate.getId().equals(id)
                )
        );

        FxRateDto dto = FxRateMapper.toDto(loaded);

        assertEquals("EUR", dto.base());
        assertEquals("USD", dto.quote());
        assertEquals(
                0,
                expectedRate.compareTo(dto.rate())
        );
    }
}
