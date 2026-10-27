package dev.rominiki.fxpulse.client;

public class FrankfurterClientException extends RuntimeException {

    private final boolean transientFailure;

    public FrankfurterClientException(String message) {
        super(message);
        this.transientFailure = false;
    }

    public FrankfurterClientException(String message, Throwable cause) {
        super(message, cause);
        this.transientFailure = false;
    }

    public FrankfurterClientException(
            String message,
            Throwable cause,
            boolean transientFailure
    ) {
        super(message, cause);
        this.transientFailure = transientFailure;
    }

    public boolean isTransientFailure() {
        return transientFailure;
    }
}
