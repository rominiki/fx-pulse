package dev.rominiki.fxpulse.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FxRateDto(
        LocalDate date,
        String base,
        String quote,
        BigDecimal rate
) {}
