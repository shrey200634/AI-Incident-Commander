package com.aiincidentcommander.query_service.exception;

public class DlqRecordNotFoundException extends RuntimeException {
    public DlqRecordNotFoundException(String message) {
        super(message);
    }
}
