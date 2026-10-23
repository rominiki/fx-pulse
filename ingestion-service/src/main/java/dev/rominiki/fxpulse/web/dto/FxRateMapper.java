package dev.rominiki.fxpulse.web.dto;

import dev.rominiki.fxpulse.domain.FxRate;

public final class FxRateMapper {

    private FxRateMapper() {
    }

    public static FxRateDto toDto(FxRate entity) {
        return new FxRateDto(
                entity.getId().getRateDate(),
                entity.getId().getBaseCurrency(),
                entity.getId().getQuoteCurrency(),
                entity.getRate()
        );
    }
}
