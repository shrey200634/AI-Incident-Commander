package com.aiincidentcommander.query_service.exception;

public class ActionNotFoundException extends  RuntimeException{
    public  ActionNotFoundException( Long id ){
        super("Action not found with this id :"  +id);
    }
}
