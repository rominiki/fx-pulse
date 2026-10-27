package dev.rominiki.fxpulse.web.dto;

import java.util.List;

public record RatePageResponse(
        List<FxRateDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) { }
