package dev.rominiki.fxpulse.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FxRateTest {

    private final FxRateId id = new FxRateId(
            "ECB",
            LocalDate.of(2025, 1, 3),
            "EUR",
            "USD"
    );

    @Test
    void shouldRejectZeroRate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FxRate(id, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldRejectNegativeRate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FxRate(id, new BigDecimal("-1.5"))
        );
    }

    @Test
    void shouldRejectNullRate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FxRate(id, null)
        );
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FxRate(null, BigDecimal.ONE)
        );
    }

    @Test
    void shouldAcceptPositiveRate() {
        FxRate fxRate = new FxRate(
                id,
                new BigDecimal("1.0299000000")
        );

        assertEquals(id, fxRate.getId());
        assertEquals(
                0,
                new BigDecimal("1.0299000000")
                        .compareTo(fxRate.getRate())
        );
    }
}
