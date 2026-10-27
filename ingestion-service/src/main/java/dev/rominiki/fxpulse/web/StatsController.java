package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.service.BasicStatsService;
import dev.rominiki.fxpulse.web.dto.BasicStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics", description = "Summary statistics for stored ECB reference rates")
public class StatsController {
    private final BasicStatsService service;

    public StatsController(BasicStatsService service) {
        this.service = service;
    }

    @GetMapping("/basic")
    @Operation(summary = "Summarize stored exchange rates",
            description = "Returns count, earliest/latest observations, minimum, maximum, " +
                    "and arithmetic mean for the selected published dates. " +
                    "Empty date ranges return count 0 and null statistics.")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @ApiResponse(responseCode = "400", description = "Invalid dates or currency")
    public BasicStatsDto summarize(
            @Parameter(description = "First included publication date, ISO format", example = "2025-01-01")
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last included publication date, ISO format", example = "2025-01-31")
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Three-letter quote currency", example = "USD")
            @RequestParam(value = "quote", defaultValue = "USD") String quote) {
        return service.summarize(from, to, quote);
    }
}
