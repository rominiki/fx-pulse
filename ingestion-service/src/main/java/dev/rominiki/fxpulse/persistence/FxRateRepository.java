package dev.rominiki.fxpulse.persistence;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FxRateRepository extends JpaRepository<FxRate, FxRateId> {

    List<FxRate> findByIdRateDateBetweenOrderByIdRateDateAsc(
            LocalDate startDate, LocalDate endDate);

    @Query(value = """
            SELECT r FROM FxRate r
            WHERE r.id.provider = :provider
              AND r.id.baseCurrency = :base
              AND r.id.quoteCurrency = :quote
              AND r.id.rateDate BETWEEN :from AND :to
            ORDER BY r.id.rateDate ASC
            """, countQuery = """
            SELECT COUNT(r) FROM FxRate r
            WHERE r.id.provider = :provider
              AND r.id.baseCurrency = :base
              AND r.id.quoteCurrency = :quote
              AND r.id.rateDate BETWEEN :from AND :to
            """)
    Page<FxRate> findPageByCurrencyAndDateRange(
            @Param("provider") String provider,
            @Param("base") String base,
            @Param("quote") String quote,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
