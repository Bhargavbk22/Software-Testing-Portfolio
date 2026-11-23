package com.lostfound.exception;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String entity, String from, String to) {
        super(entity + " cannot change status from " + from + " to " + to);
    }
}
