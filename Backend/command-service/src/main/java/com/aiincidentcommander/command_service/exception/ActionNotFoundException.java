package com.aiincidentcommander.command_service.exception;

public class ActionNotFoundException extends RuntimeException {

    public ActionNotFoundException(Long id) {

        super("Remediation action not found for the id " + id);
    }
}
