package com.aiincidentcommander.query_service.controller;


public class IncidentNotFoundException extends  RuntimeException {
    public IncidentNotFoundException(Long id ){
        super("Incident Not found with the id " + id);
    }
}
