package com.lebvest.exception;

import com.lebvest.model.dto.ErrorPayload;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j

public class GlobalExceptionHandler{

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorPayload> conflictException(ConflictException e, HttpServletRequest request) {
        return ResponseEntity.status(409).body(
                ErrorPayload.builder().message(e.getMessage())
                        .status(409)
                        .path(request.getRequestURI())
                        .build()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorPayload> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("ResourceNotFound at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(404).body(
                ErrorPayload.builder()
                        .status(404)
                        .path(request.getRequestURI())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorPayload> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        // Log the error with stack trace
        log.error("IllegalArgumentException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.badRequest().body(
                ErrorPayload.builder()
                        .status(400)
                        .path(request.getRequestURI())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorPayload> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(
                ErrorPayload.builder()
                        .status(400)
                        .path(request.getRequestURI())
                        .message("Validation failed")
                        .errors(errors)
                        .build()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorPayload> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation ->
                        errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        return ResponseEntity.badRequest().body(
                ErrorPayload.builder()
                        .status(400)
                        .path(request.getRequestURI())
                        .message("Validation failed")
                        .errors(errors)
                        .build()
        );
    }
    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorPayload> handleMessagingException(
            MessagingException e, HttpServletRequest request) {
        return ResponseEntity.status(500).body(
                ErrorPayload.builder()
                        .message("Failed to send email: " + e.getMessage())
                        .status(500)
                        .path(request.getRequestURI())
                        .build()
        );
    }

}
