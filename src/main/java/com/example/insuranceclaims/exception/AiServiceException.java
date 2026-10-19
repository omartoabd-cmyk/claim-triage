package com.example.insuranceclaims.exception;

/**
 * The AI step failed. The message is safe to show to API clients; the cause
 * (which may contain provider details) is only logged.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
