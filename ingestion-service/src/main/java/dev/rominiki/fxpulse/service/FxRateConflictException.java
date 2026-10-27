package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.domain.FxRateId;
import java.math.BigDecimal;

public class FxRateConflictException extends RuntimeException {
    public FxRateConflictException(FxRateId id, BigDecimal existing, BigDecimal incoming) {
        super("Conflicting exchange rate for " + id
                + ": stored=" + existing + ", incoming=" + incoming);
    }
}
