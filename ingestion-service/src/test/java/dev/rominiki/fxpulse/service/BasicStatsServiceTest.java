package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.persistence.BasicStatsRepository;
import dev.rominiki.fxpulse.persistence.BasicStatsRepository.StatsAggregate;
import dev.rominiki.fxpulse.web.dto.BasicStatsDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BasicStatsServiceTest {
    private final BasicStatsRepository repository = mock(BasicStatsRepository.class);
    private final BasicStatsService service = new BasicStatsService(repository);
    private static final LocalDate FROM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TO = LocalDate.of(2025, 1, 31);

    @Test
    void calculatesRoundedAverageWithoutChangingOtherStats() {
        when(repository.summarize(FROM, TO, "USD"))
                .thenReturn(new StatsAggregate(3, LocalDate.of(2025, 1, 2),
                        LocalDate.of(2025, 1, 6),
                        new BigDecimal("1.02"), new BigDecimal("1.06"),
                        new BigDecimal("1.036666666666666"), new BigDecimal("1.06")));
        BasicStatsDto result = service.summarize(FROM, TO, "USD");
        assertThat(result.observations()).isEqualTo(3);
        assertThat(result.averageRate()).isEqualByComparingTo("1.0366666667");
        assertThat(result.latestRate()).isEqualByComparingTo("1.06");
        assertThat(result.firstDate()).isEqualTo(LocalDate.of(2025, 1, 2));
    }

    @Test
    void handlesNoPublishedOrStoredObservations() {
        when(repository.summarize(FROM, TO, "GBP"))
                .thenReturn(new StatsAggregate(0, null, null, null, null, null, null));
        BasicStatsDto result = service.summarize(FROM, TO, "GBP");
        assertThat(result.observations()).isZero();
        assertThat(result.averageRate()).isNull();
        assertThat(result.latestRate()).isNull();
    }

    @Test
    void rejectsInvalidDatesOrQuoteBeforeQuerying() {
        assertThatThrownBy(() -> service.summarize(TO, FROM, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summarize(FROM, TO, "eur"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
