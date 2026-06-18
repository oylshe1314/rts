package com.sk.rts.application.exception;

import org.springframework.http.HttpStatus;

public class HttpStatusException extends RuntimeException implements StatusException {

    private final int status;
    private final String code;

    public HttpStatusException(HttpStatus status) {
        super(status.getReasonPhrase());
        this.status = status.value();
        this.code = "";
    }

    public HttpStatusException(HttpStatus status, String message) {
        super(message);
        this.status = status.value();
        this.code = "";
    }

    public HttpStatusException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status.value();
        this.code = code;
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
