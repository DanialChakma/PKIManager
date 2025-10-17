package com.pki.exception;

import org.springframework.stereotype.Component;


public class CertificateValidationException extends RuntimeException {
    private final String reason;
    private final Object details;

    public CertificateValidationException(String reason, Object details) {
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

