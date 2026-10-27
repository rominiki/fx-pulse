package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.service.BasicStatsService;
import dev.rominiki.fxpulse.web.dto.BasicStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StatsControllerTest {
    private final BasicStatsService service = mock(BasicStatsService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = standaloneSetup(new StatsController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void returnsStatsAsJson() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(service.summarize(from, to, "USD"))
                .thenReturn(new BasicStatsDto(from, to, "EUR", "USD", 2,
                        LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 3),
                        new BigDecimal("1.0200"), new BigDecimal("1.0300"),
                        new BigDecimal("1.0250"), new BigDecimal("1.0300")));
        mvc.perform(get("/api/v1/stats/basic")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("EUR"))
                .andExpect(jsonPath("$.quote").value("USD"))
                .andExpect(jsonPath("$.observations").value(2))
                .andExpect(jsonPath("$.averageRate").value(1.0250));
        verify(service).summarize(from, to, "USD");
    }

    @Test
    void badDatesAreBadRequest() throws Exception {
        mvc.perform(get("/api/v1/stats/basic")
                        .param("from", "bad-date")
                        .param("to", "2025-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        verifyNoInteractions(service);
    }
}
