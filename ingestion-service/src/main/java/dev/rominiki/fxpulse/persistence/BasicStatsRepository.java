package dev.rominiki.fxpulse.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public class BasicStatsRepository {
    private final JdbcTemplate jdbc;

    public BasicStatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** One PostgreSQL statement: the aggregate and latest value share one snapshot. */
    public StatsAggregate summarize(LocalDate from, LocalDate to, String quote) {
        String sql = """
                WITH filtered AS (
                    SELECT rate_date, rate
                    FROM raw.fx_rates
                    WHERE provider = ? AND base_currency = ?
                      AND quote_currency = ? AND rate_date BETWEEN ? AND ?
                )
                SELECT COUNT(*) AS observations,
                       MIN(rate_date) AS first_date,
                       MAX(rate_date) AS last_date,
                       MIN(rate) AS min_rate,
                       MAX(rate) AS max_rate,
                       AVG(rate) AS average_rate,
                       (SELECT rate FROM filtered ORDER BY rate_date DESC LIMIT 1)
                           AS latest_rate
                FROM filtered
                """;

        return jdbc.queryForObject(sql, (rs, rowNumber) -> new StatsAggregate(
                        rs.getLong("observations"),
                        rs.getObject("first_date", LocalDate.class),
                        rs.getObject("last_date", LocalDate.class),
                        rs.getBigDecimal("min_rate"),
                        rs.getBigDecimal("max_rate"),
                        rs.getBigDecimal("average_rate"),
                        rs.getBigDecimal("latest_rate")),
                "ECB", "EUR", quote, from, to);
    }

    public record StatsAggregate(
            long observations,
            LocalDate firstDate,
            LocalDate lastDate,
            BigDecimal minRate,
            BigDecimal maxRate,
            BigDecimal averageRate,
            BigDecimal latestRate
    ) { }
}
