package dev.rominiki.fxpulse.persistence;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class FxRateInsertRepository {

    private static final String INSERT_SQL = """
            INSERT INTO raw.fx_rates
                (provider, rate_date, base_currency, quote_currency, rate)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (provider, rate_date, base_currency, quote_currency)
            DO NOTHING
            """;

    private static final String SELECT_SQL = """
            SELECT rate FROM raw.fx_rates
            WHERE provider = ? AND rate_date = ?
              AND base_currency = ? AND quote_currency = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public FxRateInsertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertIfAbsent(FxRate rate) {
        FxRateId id = rate.getId();
        return jdbcTemplate.update(INSERT_SQL,
                id.getProvider(), id.getRateDate(),
                id.getBaseCurrency(), id.getQuoteCurrency(), rate.getRate());
    }

    public BigDecimal findStoredRate(FxRateId id) {
        List<BigDecimal> results = jdbcTemplate.query(
                SELECT_SQL,
                (rs, rowNum) -> rs.getBigDecimal(1),
                id.getProvider(), id.getRateDate(),
                id.getBaseCurrency(), id.getQuoteCurrency());

        if (results.size() != 1) {
            throw new IllegalStateException("Cannot read existing rate for " + id);
        }
        return results.getFirst();
    }
}
