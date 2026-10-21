package com.exercise.registry.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Test
    void notFoundReturns404Envelope() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants/x");

        ResponseEntity<ApiError> response = handler.notFound(new ResourceNotFoundException("gone"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ApiError body = response.getBody();
        assertEquals(404, body.status());
        assertEquals("Not Found", body.error());
        assertEquals("gone", body.message());
        assertEquals("/api/v1/tenants/x", body.path());
        assertTrue(body.details().isEmpty());
    }

    @Test
    void duplicateReturns409Envelope() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants");

        ResponseEntity<ApiError> response = handler.duplicate(new DuplicateResourceException("dup"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("dup", response.getBody().message());
    }

    @Test
    void dataIntegrityReturns409Envelope() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants/x/integrations");

        ResponseEntity<ApiError> response = handler.dataConflict(new DataIntegrityViolationException("conflict"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The request conflicts with an existing database constraint",
            response.getBody().message());
    }

    @Test
    void optimisticLockingReturns409Envelope() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants/x/integrations/y");

        ResponseEntity<ApiError> response = handler.staleUpdate(
            new OptimisticLockingFailureException("stale"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The resource changed while this request was being processed; reload and retry",
            response.getBody().message());
    }

    @Test
    void validationKeepsFirstErrorWhenFieldAppearsTwice() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createTenantRequest");
        bindingResult.addError(new FieldError("createTenantRequest", "slug", "first"));
        bindingResult.addError(new FieldError("createTenantRequest", "slug", "second"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.validation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("slug", "first"), response.getBody().details());
    }

    @Test
    void validationFallsBackWhenFieldErrorHasNoMessage() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createTenantRequest");
        bindingResult.addError(new FieldError("createTenantRequest", "name", null));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.validation(ex, request);

        assertEquals(Map.of("name", "Invalid value"), response.getBody().details());
    }

    @Test
    void invalidParameterReturns400Envelope() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants/notauuid");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "notauuid", String.class, "tenantId", (MethodParameter) null, new IllegalArgumentException());

        ResponseEntity<ApiError> response = handler.invalidParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("tenantId", "Unsupported value"), response.getBody().details());
    }

    @Test
    void malformedBodyReturns400Envelope() {
        when(request.getRequestURI()).thenReturn("/api/v1/tenants");
        ResponseEntity<ApiError> response = handler.malformedBody(
            new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request body is invalid or contains an unsupported enum value",
            response.getBody().message());
    }
}
