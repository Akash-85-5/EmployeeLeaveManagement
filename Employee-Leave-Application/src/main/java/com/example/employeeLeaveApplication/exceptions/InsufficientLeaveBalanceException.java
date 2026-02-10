package com.example.employeeLeaveApplication.exceptions;

public class InsufficientLeaveBalanceException extends RuntimeException {

    public InsufficientLeaveBalanceException() {
        super("Insufficient leave balance");
    }

    public InsufficientLeaveBalanceException(String message) {
        super(message);
    }
}
