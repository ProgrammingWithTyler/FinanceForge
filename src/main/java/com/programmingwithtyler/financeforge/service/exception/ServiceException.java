package com.programmingwithtyler.financeforge.service.exception;

/**
 * Base exception for all service layer exceptions.
 */
public abstract class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
