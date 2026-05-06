package com.insurance.engine.exception;

import com.insurance.engine.api.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex) {
        String message = ex.getMessage();
        log.error("Error: {}", message);

        if (message != null) {
            if (message.startsWith("SESSION_NOT_FOUND")) {
                return error(HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND",
                        "Quote session not found");
            }
            if (message.startsWith("QUOTE_NOT_FOUND")) {
                return error(HttpStatus.NOT_FOUND,
                        "QUOTE_NOT_FOUND",
                        "Quote not found");
            }
            if (message.startsWith("QUOTE_EXPIRED")) {
                return error(HttpStatus.GONE,
                        "QUOTE_EXPIRED",
                        "This quote has expired. " +
                        "Please request a new quote.");
            }
            if (message.startsWith("API1_UNAVAILABLE")) {
                return error(HttpStatus.SERVICE_UNAVAILABLE,
                        "API1_UNAVAILABLE",
                        "Quote Session Service is unavailable. " +
                        "Please try again.");
            }
            if (message.startsWith("VALIDATION_ERROR")) {
                return error(HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        message.replace("VALIDATION_ERROR: ", ""));
            }
        }

        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status, String code, String message) {
        ErrorResponse response = new ErrorResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(OffsetDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}