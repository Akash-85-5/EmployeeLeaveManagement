package com.emp_management.shared.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String errorCode;      // e.g. "EMPLOYEE_NOT_FOUND"
    private String message;        // safe, user-facing message
    private Map<String, String> fieldErrors; // only for validation errors

    private ErrorResponse() {}

    public static ErrorResponse of(int status, ErrorCode code, String message) {
        ErrorResponse r = new ErrorResponse();
        r.timestamp = LocalDateTime.now();
        r.status = status;
        r.errorCode = code.name();
        r.message = message;
        return r;
    }

    public static ErrorResponse withFields(int status, ErrorCode code,
                                           String message,
                                           Map<String, String> fieldErrors) {
        ErrorResponse r = of(status, code, message);
        r.fieldErrors = fieldErrors;
        return r;
    }

    // getters only — no setters exposed
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}