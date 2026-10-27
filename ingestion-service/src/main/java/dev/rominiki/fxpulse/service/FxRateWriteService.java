package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.persistence.FxRateInsertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FxRateWriteService {

    private final FxRateInsertRepository insertRepository;

    public FxRateWriteService(FxRateInsertRepository insertRepository) {
        this.insertRepository = insertRepository;
    }

    @Transactional
    public WriteCounts persist(List<FxRate> rates) {
        int inserted = 0;
        int skipped = 0;

        for (FxRate rate : rates) {
            int changedRows = insertRepository.insertIfAbsent(rate);
            if (changedRows == 1) {
                inserted++;
            } else if (changedRows == 0) {
                BigDecimal stored = insertRepository.findStoredRate(rate.getId());
                if (stored.compareTo(rate.getRate()) != 0) {
                    throw new FxRateConflictException(rate.getId(), stored, rate.getRate());
                }
                skipped++;
            } else {
                throw new IllegalStateException("Unexpected insert count: " + changedRows);
            }
        }
        return new WriteCounts(inserted, skipped);
    }

    public record WriteCounts(int inserted, int skipped) {}
}
