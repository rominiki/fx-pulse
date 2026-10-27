package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import dev.rominiki.fxpulse.persistence.FxRateRepository;
import dev.rominiki.fxpulse.web.dto.RatePageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FxRateQueryServiceTest {
    private final FxRateRepository repository = mock(FxRateRepository.class);
    private final FxRateQueryService service = new FxRateQueryService(repository);
    private static final LocalDate FROM = LocalDate.of(2025, 1, 2);
    private static final LocalDate TO = LocalDate.of(2025, 1, 31);

    @Test
    void mapsRowsAndPassesPaginationToRepository() {
        FxRate first = new FxRate(new FxRateId("ECB", FROM, "EUR", "USD"),
                new BigDecimal("1.0299000000"));
        FxRate second = new FxRate(new FxRateId("ECB", FROM.plusDays(1), "EUR", "USD"),
                new BigDecimal("1.0301000000"));
        when(repository.findPageByCurrencyAndDateRange(eq("ECB"), eq("EUR"), eq("USD"),
                eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second),
                        org.springframework.data.domain.PageRequest.of(0, 2), 5));

        RatePageResponse result = service.findRates(FROM, TO, "USD", 0, 2);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().getFirst().base()).isEqualTo("EUR");
        assertThat(result.content().getFirst().rate()).isEqualByComparingTo("1.0299");
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findPageByCurrencyAndDateRange(eq("ECB"), eq("EUR"),
                eq("USD"), eq(FROM), eq(TO), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(2);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void emptyDatabaseReturnsEmptyPage() {
        when(repository.findPageByCurrencyAndDateRange(eq("ECB"), eq("EUR"), eq("GBP"),
                eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty(
                        org.springframework.data.domain.PageRequest.of(0, 20)));
        RatePageResponse result = service.findRates(FROM, TO, "GBP", 0, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void rejectsInvalidInputsBeforeQuerying() {
        assertThatThrownBy(() -> service.findRates(TO, FROM, "USD", 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findRates(FROM, TO, "usd", 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findRates(FROM, TO, "EUR", 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findRates(FROM, TO, "USD", -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findRates(FROM, TO, "USD", 0, 101))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
