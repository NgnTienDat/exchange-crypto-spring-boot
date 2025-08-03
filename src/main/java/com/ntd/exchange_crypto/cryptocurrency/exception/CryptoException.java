package com.ntd.exchange_crypto.cryptocurrency.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CryptoException extends RuntimeException {
    private CryptoErrorCode errorCode;
    public CryptoException(CryptoErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
