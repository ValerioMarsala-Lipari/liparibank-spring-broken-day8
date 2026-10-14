package com.lipari.bank.shared.exception;

import org.springframework.http.HttpStatus;

public sealed class LipariBankException extends RuntimeException
        permits ResourceNotFoundException, BusinessException, ConflictException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected LipariBankException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    protected LipariBankException(String message, HttpStatus httpStatus,
                                   String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
