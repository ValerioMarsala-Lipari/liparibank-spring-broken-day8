package com.lipari.bank.shared.exception;

import org.springframework.http.HttpStatus;

public final class ConflictException extends LipariBankException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
