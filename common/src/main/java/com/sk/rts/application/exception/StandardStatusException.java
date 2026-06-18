package com.sk.rts.application.exception;

public class StandardStatusException extends RuntimeException implements StatusException {

    private final int status;
    private final String code;

    public StandardStatusException() {
        super(ResponseStatus.unknown_error.getMessage());
        this.status = ResponseStatus.unknown_error.getStatus();
        this.code = ResponseStatus.unknown_error.getCode();
    }

    public StandardStatusException(StatusException statusException) {
        super(statusException.getMessage());
        this.status = statusException.getStatus();
        this.code = statusException.getCode();
    }

    public StandardStatusException(StatusException statusException, String message) {
        super(message);
        this.status = statusException.getStatus();
        this.code = statusException.getCode();
    }

    public StandardStatusException(String message) {
        super(message);
        this.status = ResponseStatus.failure.getStatus();
        this.code = ResponseStatus.failure.getCode();
    }

    public StandardStatusException(int status, String message) {
        super(message);
        this.status = status;
        this.code = null;
    }

    public StandardStatusException(String code, String message) {
        super(message);
        this.status = ResponseStatus.failure.getStatus();
        this.code = code;
    }

    public StandardStatusException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public StandardStatusException(Throwable cause) {
        super(cause.getMessage());
        StatusException statusException = StatusException.extractStatusException(cause);
        if (statusException != null) {
            this.status = statusException.getStatus();
            this.code = statusException.getCode();
        } else {
            this.status = ResponseStatus.unknown_error.getStatus();
            this.code = ResponseStatus.unknown_error.getCode();
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }
}
