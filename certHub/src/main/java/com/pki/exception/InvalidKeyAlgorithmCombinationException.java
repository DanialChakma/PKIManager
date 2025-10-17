package com.pki.exception;


public class InvalidKeyAlgorithmCombinationException extends RuntimeException {
    private final String reason;
    private final Object details;

    public InvalidKeyAlgorithmCombinationException(String message) {
        super(message);
        this.reason = message;
        this.details = null;
    }


    public InvalidKeyAlgorithmCombinationException(String reason, Object details) {
        super(reason);
        this.reason = reason;
        this.details = details;
    }

    public String getReason() {
        return reason;
    }

    public Object getDetails() {
        return details;
    }
}

