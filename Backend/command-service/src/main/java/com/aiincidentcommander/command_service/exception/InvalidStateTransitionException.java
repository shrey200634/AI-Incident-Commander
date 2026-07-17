package com.aiincidentcommander.command_service.exception;

public class InvalidStateTransitionException extends  RuntimeException{

    public  InvalidStateTransitionException(String form , String to ){
        super("Invalid transition from " + form + "to" + to);
    }
}
