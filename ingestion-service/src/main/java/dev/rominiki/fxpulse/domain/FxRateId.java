package dev.rominiki.fxpulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@EqualsAndHashCode
@Getter
public class FxRateId implements Serializable {

    @Column(name = "provider", length = 16, nullable = false)
    private String provider;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;


    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "base_currency",
            columnDefinition = "CHAR(3)",
            length = 3,
            nullable = false
    )
    private String baseCurrency;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "quote_currency",
            columnDefinition = "CHAR(3)",
            length = 3,
            nullable = false
    )
    private String quoteCurrency;
}
