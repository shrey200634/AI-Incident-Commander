package com.aiincidentcommander.command_service.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String from, String to) {
        super("Invalid transition from " + from + " to " + to);
    }
}

