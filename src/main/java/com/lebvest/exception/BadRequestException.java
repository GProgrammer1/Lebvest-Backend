package com.lebvest.exception;

/**
 * Exception thrown when a bad request is made (e.g., validation failures, invalid input).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
