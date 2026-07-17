package com.aiincidentcommander.command_service.exception;


public class IncidentNotFoundException extends  RuntimeException{
    public  IncidentNotFoundException(Long id ){
        super("Incident not found " + id);
    }
}
