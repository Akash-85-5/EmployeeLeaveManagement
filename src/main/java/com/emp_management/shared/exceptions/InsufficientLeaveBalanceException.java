package com.emp_management.shared.exceptions;

public class InsufficientLeaveBalanceException extends RuntimeException {

    public InsufficientLeaveBalanceException() {
        super("You do not have enough leave balance.");
    }

    public InsufficientLeaveBalanceException(String message) {
        super(message);
    }
}