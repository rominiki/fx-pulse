package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.service.FxRateQueryService;
import dev.rominiki.fxpulse.web.dto.RatePageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rates")
public class FxRateController {
    private final FxRateQueryService queryService;

    public FxRateController(FxRateQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public RatePageResponse findRates(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "quote", defaultValue = "USD") String quote,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return queryService.findRates(from, to, quote, page, size);
    }
}
