package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.client.FrankfurterClientException;
import dev.rominiki.fxpulse.service.BackfillService;
import dev.rominiki.fxpulse.service.FxRateIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class IngestionErrorHandlerTest {
    private final FxRateIngestionService ingestion = mock(FxRateIngestionService.class);
    private final BackfillService backfill = mock(BackfillService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = standaloneSetup(new IngestionController(ingestion, backfill))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void transientUpstreamFailureBecomes503() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 2);
        when(ingestion.ingest(date, date, "USD"))
                .thenThrow(new FrankfurterClientException("Frankfurter HTTP 503",
                        new RuntimeException("outage"), true));
        mvc.perform(post("/api/v1/ingestion").param("date", "2025-01-02"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("UPSTREAM_UNAVAILABLE"));
    }

    @Test
    void unrecoverableUpstreamErrorBecomes502() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 2);
        when(ingestion.ingest(date, date, "USD"))
                .thenThrow(new FrankfurterClientException("Invalid source response"));
        mvc.perform(post("/api/v1/ingestion").param("date", "2025-01-02"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_FAILURE"));
    }
}
