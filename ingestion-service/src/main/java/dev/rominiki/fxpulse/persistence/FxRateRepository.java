package dev.rominiki.fxpulse.persistence;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.domain.FxRateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FxRateRepository  extends JpaRepository<FxRate, FxRateId> {

    List<FxRate> findByIdRateDateBetweenOrderByIdRateDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );
}
