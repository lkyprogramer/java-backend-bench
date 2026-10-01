package com.orderdesk.service;

import org.springframework.http.HttpStatus;

/** Expected business failure; mapped to a JSON error by the web layer. */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static BusinessException notFound(String what, Object id) {
        return new BusinessException(HttpStatus.NOT_FOUND, what.toUpperCase() + "_NOT_FOUND", what + " " + id + " not found");
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }
}
