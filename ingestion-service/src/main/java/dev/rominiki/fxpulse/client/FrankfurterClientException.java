package dev.rominiki.fxpulse.client;

public class FrankfurterClientException extends RuntimeException {

    public FrankfurterClientException(String message) {
        super(message);
    }

    public FrankfurterClientException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
