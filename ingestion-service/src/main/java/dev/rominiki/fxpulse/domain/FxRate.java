package dev.rominiki.fxpulse.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fx_rates" , schema = "raw")
@NoArgsConstructor
@Getter
public class FxRate {

    @EmbeddedId
    private FxRateId id;

    @Column(
            name = "rate",
            precision = 20,
            scale = 10,
            nullable = false
    )
    private BigDecimal rate;

    @Column(
            name = "ingested_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime ingestedAt;


    public FxRate(FxRateId id, BigDecimal rate) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "FxRateId cannot be null"
            );
        }

        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Exchange rate must be positive"
            );
        }

        this.id = id;
        this.rate = rate;
    }
}
