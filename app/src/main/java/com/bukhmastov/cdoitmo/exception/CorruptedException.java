package com.bukhmastov.cdoitmo.exception;

public class CorruptedException extends Exception {
    public CorruptedException() {}
    public CorruptedException(String message) {
        super(message);
    }
    public CorruptedException(Throwable cause) {
        super(cause);
    }
}
