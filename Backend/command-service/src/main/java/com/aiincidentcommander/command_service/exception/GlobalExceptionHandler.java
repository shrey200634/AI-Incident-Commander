package com.aiincidentcommander.command_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex ){
        return buildResponse(HttpStatus.NOT_FOUND , ex.getMessage());
    }

    @ExceptionHandler(ActionNnotFoundException.class)
    public  ResponseEntity<Map<String , Object>> handleActionNotFound(RuntimeException ex ){
        return buildResponse(HttpStatus.NOT_FOUND , ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public  ResponseEntity<Map<String,Object>> handleInvalidTransition(RuntimeException ex ){
        return  buildResponse(HttpStatus.CONFLICT , ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class )
    public  ResponseEntity<Map<String , Object>> handleGeneric(Exception ex ){
        log.error("unhandled Exception " , ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "something went wrong check the logs ");
    }

    private ResponseEntity<Map<String , Object>> buildResponse(HttpStatus status , String message ){
        Map<String , Object> body = new HashMap<>();
        body.put("timestamp" , LocalDateTime.now());
        body.put("status " , status.value());
        body.put("error" , message);
        return ResponseEntity.status(status).body(body);
    }
}
