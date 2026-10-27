package dev.rominiki.fxpulse.web;

import dev.rominiki.fxpulse.client.FrankfurterClientException;
import dev.rominiki.fxpulse.service.FxRateConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_INPUT", exception.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> invalidParameter(Exception exception) {
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_PARAMETER", "A required query parameter is missing or invalid"));
    }

    @ExceptionHandler(FxRateConflictException.class)
    public ResponseEntity<ApiError> conflict(FxRateConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("RATE_CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(FrankfurterClientException.class)
    public ResponseEntity<ApiError> upstream(FrankfurterClientException exception) {
        if (exception.isTransientFailure()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("UPSTREAM_UNAVAILABLE", exception.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("UPSTREAM_FAILURE", exception.getMessage()));
    }
}
