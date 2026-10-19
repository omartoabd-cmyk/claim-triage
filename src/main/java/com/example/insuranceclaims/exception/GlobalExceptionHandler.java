package com.example.insuranceclaims.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.insuranceclaims.dto.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ClaimNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(404, e.getMessage()));
    }

    @ExceptionHandler(DuplicateClaimException.class)
    public ResponseEntity<ApiError> duplicate(DuplicateClaimException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "The claim conflicts with existing data."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, "Validation failed", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "Malformed request."));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> rateLimited(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(ApiError.of(429, e.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiError> ai(AiServiceException e) {
        log.warn("AI analysis failed: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.of(503, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        // Spring's own MVC exceptions (404 no such route, 405, 415, ...) carry their
        // intended status; don't turn them into 500s.
        if (e instanceof ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode())
                    .headers(er.getHeaders())
                    .body(ApiError.of(er.getStatusCode().value(), er.getBody().getDetail()));
        }
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Unexpected server error."));
    }
}
