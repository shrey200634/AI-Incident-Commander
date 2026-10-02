package com.aiincidentcommander.command_service.exception;

public class UnsupportedActionTypeException extends  RuntimeException{
    public  UnsupportedActionTypeException(String message ){
        super("Unsupported action type: '" + message + "'. Allowed: RESTART_SERVICE, SCALE_SERVICE, "
                + "ROLLBACK_DEPLOYMENT, CLEAR_KAFKA_DLQ");
    }
}
