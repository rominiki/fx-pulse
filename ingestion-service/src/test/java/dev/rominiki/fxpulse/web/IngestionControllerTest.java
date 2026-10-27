package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.service.BackfillResult;
import dev.rominiki.fxpulse.service.BackfillService;
import dev.rominiki.fxpulse.service.FxRateIngestionService;
import dev.rominiki.fxpulse.service.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class IngestionControllerTest {
    private final FxRateIngestionService ingestion = mock(FxRateIngestionService.class);
    private final BackfillService backfill = mock(BackfillService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = standaloneSetup(new IngestionController(ingestion, backfill))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void singleDayEndpointReturnsCounts() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 2);
        when(ingestion.ingest(date, date, "USD"))
                .thenReturn(new IngestionResult(1, 1, 0, 0));
        mvc.perform(post("/api/v1/ingestion").param("date", "2025-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(1));
        verify(ingestion).ingest(date, date, "USD");
    }

    @Test
    void backfillEndpointReturnsWindowSummary() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(backfill.backfill(from, to, "GBP"))
                .thenReturn(new BackfillResult(from, to, "GBP", 2, 2, 0, 0, List.of()));
        mvc.perform(post("/api/v1/ingestion/backfill")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31")
                        .param("quote", "GBP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(2))
                .andExpect(jsonPath("$.quote").value("GBP"));
    }

    @Test
    void invalidDateIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/ingestion").param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ingestion);
    }

    @Test
    void malformedRangeIsBadRequest() throws Exception {
        LocalDate from = LocalDate.of(2025, 2, 2);
        LocalDate to = LocalDate.of(2025, 1, 1);
        when(backfill.backfill(from, to, "USD"))
                .thenThrow(new IllegalArgumentException("Invalid backfill date range"));
        mvc.perform(post("/api/v1/ingestion/backfill")
                        .param("from", "2025-02-02")
                        .param("to", "2025-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
