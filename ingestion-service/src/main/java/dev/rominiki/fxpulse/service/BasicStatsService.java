package dev.rominiki.fxpulse.service;

import dev.rominiki.fxpulse.persistence.BasicStatsRepository;
import dev.rominiki.fxpulse.persistence.BasicStatsRepository.StatsAggregate;
import dev.rominiki.fxpulse.web.dto.BasicStatsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class BasicStatsService {
    private final BasicStatsRepository repository;

    public BasicStatsService(BasicStatsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BasicStatsDto summarize(LocalDate from, LocalDate to, String quote) {
        RateQueryValidator.validate(from, to, quote);
        StatsAggregate data = repository.summarize(from, to, quote);
        BigDecimal average = data.averageRate() == null ? null :
                data.averageRate().setScale(10, RoundingMode.HALF_UP);
        return new BasicStatsDto(from, to, "EUR", quote,
                data.observations(), data.firstDate(), data.lastDate(),
                data.minRate(), data.maxRate(), average, data.latestRate());
    }
}
