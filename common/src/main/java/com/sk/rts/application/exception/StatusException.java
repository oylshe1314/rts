package com.sk.rts.application.exception;

public interface StatusException {

    static StatusException extractStatusException(Throwable cause) {
        while (cause != null) {
            if (cause instanceof StatusException) {
                return (StatusException) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }

    int getStatus();

    String getCode();

    String getMessage();
}
