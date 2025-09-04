package com.lebvest.exception;

import com.lebvest.model.dto.ErrorPayload;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
