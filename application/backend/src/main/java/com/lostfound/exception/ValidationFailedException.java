package com.lostfound.exception;

import java.util.Map;

public class ValidationFailedException extends RuntimeException {
    private final Map<String, String> errors;

    public ValidationFailedException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() { return errors; }
}
