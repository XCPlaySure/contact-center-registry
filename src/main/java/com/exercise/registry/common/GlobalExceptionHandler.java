package com.exercise.registry.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(
        ResourceNotFoundException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> duplicate(
        DuplicateResourceException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataConflict(
        DataIntegrityViolationException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, 
            "The request conflicts with an existing database constraint", 
            request, Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> staleUpdate(
        OptimisticLockingFailureException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, 
            "The resource changed while this request was being processed; reload and retry", 
            request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(
        MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                    FieldError::getField,
                    error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
                    (first, ignored) -> first,
                    LinkedHashMap::new));
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", request, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> invalidParameter(
        MethodArgumentTypeMismatchException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, 
            "Invalid request parameter or path variable: " + ex.getName(), 
            request, Map.of(ex.getName(), "Unsupported value"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedBody(
        HttpMessageNotReadableException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, 
            "Request body is invalid or contains an unsupported enum value", 
            request, Map.of());
    }

    private ResponseEntity<ApiError> build(
        HttpStatus status, String message, HttpServletRequest request, Map<String, String> details
    ) {
        ApiError error = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(status).body(error);
    }
}
