package com.programmingwithtyler.financeforge.api.exception;

import com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.InactiveAccountException;
import com.programmingwithtyler.financeforge.service.exception.InsufficientFundsException;
import com.programmingwithtyler.financeforge.service.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent API error responses.
 *
 * Maps domain exceptions to appropriate HTTP status codes:
 * - 400 BAD REQUEST: Validation failures, invalid input
 * - 404 NOT FOUND: Resource not found
 * - 409 CONFLICT: Business rule violations (e.g., insufficient funds, inactive account)
 *
 * All error responses include:
 * - timestamp: When the error occurred
 * - status: HTTP status code
 * - error: HTTP status reason phrase
 * - message: Human-readable error description
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid request body validation.
     *
     * Returns 400 BAD REQUEST with field-level error details.
     *
     * Example response:
     * {
     *   "timestamp": "2026-01-31T10:15:30",
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Validation failed",
     *   "errors": {
     *     "amount": "Amount must be positive",
     *     "transactionDate": "Transaction date cannot be in the future"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.put("message", "Validation failed");
        errorResponse.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle insufficient funds during expense or transfer.
     *
     * Returns 409 CONFLICT with current balance and requested amount.
     *
     * Example response:
     * {
     *   "timestamp": "2026-01-31T10:15:30",
     *   "status": 409,
     *   "error": "Conflict",
     *   "message": "Insufficient funds in account 1. Current balance: 1500.00, Requested: 2000.00"
     * }
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
        InsufficientFundsException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle inactive account errors.
     *
     * Returns 409 CONFLICT (business rule violation).
     *
     * Example response:
     * {
     *   "timestamp": "2026-01-31T10:15:30",
     *   "status": 409,
     *   "error": "Conflict",
     *   "message": "Account 1 is inactive and cannot be used for transactions"
     * }
     */
    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<Map<String, Object>> handleInactiveAccount(
        InactiveAccountException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle account not found errors.
     *
     * Returns 404 NOT FOUND.
     *
     * Example response:
     * {
     *   "timestamp": "2026-01-31T10:15:30",
     *   "status": 404,
     *   "error": "Not Found",
     *   "message": "Account not found with ID: 999"
     * }
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(
        AccountNotFoundException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle transaction not found errors.
     *
     * Returns 404 NOT FOUND.
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionNotFound(
        TransactionNotFoundException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions (e.g., invalid date ranges).
     *
     * Returns 400 BAD REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
        IllegalArgumentException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
