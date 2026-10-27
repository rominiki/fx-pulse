package dev.rominiki.fxpulse.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BackfillServiceTest {
    private final FxRateIngestionService ingestion = mock(FxRateIngestionService.class);
    private final BackfillService service = new BackfillService(ingestion);

    @Test
    void splitsAtMonthBoundariesAndAggregatesCounts() {
        LocalDate jan30 = LocalDate.of(2025, 1, 30);
        LocalDate jan31 = LocalDate.of(2025, 1, 31);
        LocalDate feb1 = LocalDate.of(2025, 2, 1);
        LocalDate feb3 = LocalDate.of(2025, 2, 3);
        when(ingestion.ingest(jan30, jan31, "USD"))
                .thenReturn(new IngestionResult(2, 2, 0, 0));
        when(ingestion.ingest(feb1, feb3, "USD"))
                .thenReturn(new IngestionResult(2, 1, 1, 0));

        BackfillResult result = service.backfill(jan30, feb3, "USD");

        assertThat(result.windows()).hasSize(2);
        assertThat(result.received()).isEqualTo(4);
        assertThat(result.inserted()).isEqualTo(3);
        assertThat(result.skippedDuplicates()).isEqualTo(1);
        verify(ingestion).ingest(jan30, jan31, "USD");
        verify(ingestion).ingest(feb1, feb3, "USD");
    }

    @Test
    void supportsOneYearWithoutSingleGiantRequest() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(ingestion.ingest(any(LocalDate.class), any(LocalDate.class), eq("USD")))
                .thenReturn(new IngestionResult(0, 0, 0, 0));
        BackfillResult result = service.backfill(from, to, "USD");
        assertThat(result.windows()).hasSize(12);
        verify(ingestion, times(12)).ingest(any(LocalDate.class), any(LocalDate.class), eq("USD"));
    }

    @Test
    void rejectsInvalidAndExcessiveRangesBeforeFetching() {
        assertThatThrownBy(() -> service.backfill(
                LocalDate.of(2025, 2, 2), LocalDate.of(2025, 1, 1), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.backfill(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.backfill(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1), "eur"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(ingestion);
    }

    @Test
    void failsFastIfAnyWindowFails() {
        LocalDate from = LocalDate.of(2025, 1, 31);
        LocalDate feb1 = LocalDate.of(2025, 2, 1);
        LocalDate feb28 = LocalDate.of(2025, 2, 28);
        when(ingestion.ingest(from, from, "USD"))
                .thenReturn(new IngestionResult(1, 1, 0, 0));
        when(ingestion.ingest(feb1, feb28, "USD"))
                .thenThrow(new IllegalStateException("upstream failed"));
        assertThatThrownBy(() -> service.backfill(from, LocalDate.of(2025, 3, 2), "USD"))
                .isInstanceOf(IllegalStateException.class);
        verify(ingestion, never()).ingest(any(), eq(LocalDate.of(2025, 3, 2)), any());
    }
}
