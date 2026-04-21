package com.emp_management.shared.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Auth ──────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.BAD_CREDENTIAL,
                        "Invalid username or password."));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return respond(HttpStatus.FORBIDDEN,
                ErrorResponse.of(403, ErrorCode.FORBIDDEN, ex.getMessage()));
    }

    // ── Application-level ─────────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND,
                ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientLeaveBalanceException.class)
    public ResponseEntity<ErrorResponse> handleLeave(InsufficientLeaveBalanceException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INSUFFICIENT_LEAVE_BALANCE, ex.getMessage()));
    }

    // ── JPA ───────────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJpa(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return respond(HttpStatus.NOT_FOUND,
                ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND,
                        ex.getMessage() != null ? ex.getMessage() : "Record not found."));
    }

    // ── Spring MVC — request binding ──────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));

        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.withFields(400, ErrorCode.VALIDATION_FAILED,
                        "Validation failed. Please check the highlighted fields.", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonError(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Invalid request body. Please check your JSON format."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Required parameter missing: " + ex.getParameterName()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        // Fires when a required @RequestPart is absent in a multipart request
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Required file/part missing: " + ex.getRequestPartName()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Required header missing: " + ex.getHeaderName()));
    }

    // ── Spring MVC — routing ──────────────────────────────────────

    /**
     * Fired when the URL exists but the HTTP method is wrong.
     * e.g. sending PUT to a GET-only endpoint.
     * Previously fell through to the 500 handler — now correctly returns 405.
     *
     * This is also the handler for the "PUT not supported" error you saw:
     * it means the URL the frontend called does not match any registered
     * PUT mapping in your controllers. Check the exact URL being called.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleWrongMethod(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedMethods() != null
                ? String.join(", ", ex.getSupportedMethods()) : "unknown";
        log.warn("Method not allowed: {} — supported: {}", ex.getMethod(), allowed);
        return respond(HttpStatus.METHOD_NOT_ALLOWED,
                ErrorResponse.of(405, ErrorCode.INVALID_REQUEST,
                        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. "
                                + "Allowed: " + allowed));
    }

    /**
     * Fired when no controller mapping exists for the requested URL at all.
     * Requires spring.mvc.throw-exception-if-no-handler-found=true
     * and spring.web.resources.add-mappings=false in application.properties.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException ex) {
        log.warn("No handler: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return respond(HttpStatus.NOT_FOUND,
                ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND,
                        "Endpoint not found: " + ex.getHttpMethod() + " " + ex.getRequestURL()));
    }

    // ── File upload ───────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Uploaded file is too large. Maximum allowed size is 15 MB."));
    }

    // ── Final safety net ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorResponse.of(500, ErrorCode.UNEXPECTED_ERROR,
                        "Something went wrong on our end. Please try again later."));
    }

    // ── Helper ────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, ErrorResponse body) {
        return new ResponseEntity<>(body, status);
    }
}