package com.emp_management.shared.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST, ex.getMessage()));
    }

    // ✅ Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return respond(HttpStatus.FORBIDDEN,
                ErrorResponse.of(403, ErrorCode.UNAUTHORIZED_ACCESS, ex.getMessage()));
    }

    // ✅ Leave
    @ExceptionHandler(InsufficientLeaveBalanceException.class)
    public ResponseEntity<ErrorResponse> handleLeave(InsufficientLeaveBalanceException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INSUFFICIENT_LEAVE_BALANCE, ex.getMessage()));
    }

    // ✅ Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND,
                ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage()));
    }

    // ✅ Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.withFields(
                        400,
                        ErrorCode.VALIDATION_FAILED,
                        "Validation failed",
                        fieldErrors
                ));
    }

    // ✅ Bad JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonError(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Invalid request body format"));
    }

    // ✅ Missing params
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "Missing parameter: " + ex.getParameterName()));
    }

    // ✅ File size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFile(MaxUploadSizeExceededException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of(400, ErrorCode.INVALID_REQUEST,
                        "File size too large"));
    }

    // ✅ JPA not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJpa(EntityNotFoundException ex) {
        log.warn("JPA error: {}", ex.getMessage());
        return respond(HttpStatus.NOT_FOUND,
                ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND,
                        "Record not found"));
    }

    // 🔥 Final safety net
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);

        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorResponse.of(500, ErrorCode.UNEXPECTED_ERROR,
                        "Something went wrong. Please try again."));
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, ErrorResponse body) {
        return new ResponseEntity<>(body, status);
    }
}