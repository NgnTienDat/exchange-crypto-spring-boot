package com.ntd.exchange_crypto.auth.exception;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AuthException extends RuntimeException {
    private AuthErrorCode errorCode;
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}





