package dev.rominiki.fxpulse.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Null numeric summary fields mean no ECB rates were published/stored in the range. */
public record BasicStatsDto(
        LocalDate from,
        LocalDate to,
        String base,
        String quote,
        long observations,
        LocalDate firstDate,
        LocalDate lastDate,
        BigDecimal minRate,
        BigDecimal maxRate,
        BigDecimal averageRate,
        BigDecimal latestRate
) { }
