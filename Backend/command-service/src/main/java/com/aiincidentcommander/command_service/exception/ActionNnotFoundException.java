package com.aiincidentcommander.command_service.exception;

    public class ActionNnotFoundException extends  RuntimeException{

        public  ActionNnotFoundException(Long id ){
            super("Remediation action not found  for the id " + id );
        }
    }


