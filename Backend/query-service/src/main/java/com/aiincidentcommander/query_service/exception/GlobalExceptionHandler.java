package com.aiincidentcommander.query_service.exception;

import com.aiincidentcommander.query_service.exception.ActionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<Map<String , Object>> handleIncidentNotFound(IncidentNotFoundException ex ){
        return buildResponse(HttpStatus.NOT_FOUND , ex.getMessage());
    }

    @ExceptionHandler(ActionNotFoundException.class)
    public  ResponseEntity<Map<String,Object>> handleActionNotFound( ActionNotFoundException ex){
        return buildResponse(HttpStatus.NOT_FOUND , ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String , Object>> handleGeneric(Exception ex ){
        log.error("unhandled exception" , ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR , "something went wrong check the logs ");
    }



    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status , String message ){
        Map<String , Object> body = new HashMap<>();
        body.put("timestamp" , LocalDateTime.now());
        body.put("status" , status.value());
        body.put("error" , message);
        return ResponseEntity.status(status).body(body);
    }
}
