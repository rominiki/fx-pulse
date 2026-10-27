package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.service.FxRateQueryService;
import dev.rominiki.fxpulse.web.dto.FxRateDto;
import dev.rominiki.fxpulse.web.dto.RatePageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class FxRateControllerTest {
    private final FxRateQueryService service = mock(FxRateQueryService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = standaloneSetup(new FxRateController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsFilteredRatePage() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 2);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(service.findRates(from, to, "USD", 0, 2)).thenReturn(
                new RatePageResponse(List.of(new FxRateDto(from, "EUR", "USD",
                        new BigDecimal("1.0289"))), 0, 2, 1, 1));

        mvc.perform(get("/api/v1/rates")
                        .param("from", "2025-01-02")
                        .param("to", "2025-01-31")
                        .param("quote", "USD")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quote").value("USD"))
                .andExpect(jsonPath("$.content[0].rate").value(1.0289))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(service).findRates(from, to, "USD", 0, 2);
    }

    @Test
    void defaultsQuotePageAndSize() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 2);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(service.findRates(from, to, "USD", 0, 20))
                .thenReturn(new RatePageResponse(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/v1/rates")
                .param("from", "2025-01-02")
                .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        verify(service).findRates(from, to, "USD", 0, 20);
    }

    @Test
    void invalidDateAndMissingDatesReturn400() throws Exception {
        mvc.perform(get("/api/v1/rates").param("from", "not-a-date")
                .param("to", "2025-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        mvc.perform(get("/api/v1/rates").param("from", "2025-01-02"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void invalidPageSizeReturns400() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 2);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(service.findRates(from, to, "USD", 0, 999))
                .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));
        mvc.perform(get("/api/v1/rates").param("from", "2025-01-02")
                        .param("to", "2025-01-31").param("size", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
