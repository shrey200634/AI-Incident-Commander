package com.aiincidentcommander.query_service.exception;


public class IncidentNotFoundException extends  RuntimeException {
    public IncidentNotFoundException(Long id ){
        super("Incident Not found with the id " + id);
    }
}
