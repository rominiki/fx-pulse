package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.domain.FxRate;
import dev.rominiki.fxpulse.persistence.FxRateRepository;
import dev.rominiki.fxpulse.web.dto.FxRateDto;
import dev.rominiki.fxpulse.web.dto.FxRateMapper;
import dev.rominiki.fxpulse.web.dto.RatePageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FxRateQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE_NUMBER = 100_000;
    private final FxRateRepository repository;

    public FxRateQueryService(FxRateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RatePageResponse findRates(LocalDate from, LocalDate to, String quote,
                                      int page, int size) {
        RateQueryValidator.validate(from, to, quote);
        if (page < 0 || page > MAX_PAGE_NUMBER) {
            throw new IllegalArgumentException("page must be between 0 and " + MAX_PAGE_NUMBER);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Page<FxRate> result = repository.findPageByCurrencyAndDateRange(
                "ECB", "EUR", quote, from, to, PageRequest.of(page, size));
        List<FxRateDto> items = result.getContent().stream()
                .map(FxRateMapper::toDto)
                .toList();
        return new RatePageResponse(items, page, size,
                result.getTotalElements(), result.getTotalPages());
    }
}
