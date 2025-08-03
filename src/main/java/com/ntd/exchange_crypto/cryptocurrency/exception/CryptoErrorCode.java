package com.ntd.exchange_crypto.cryptocurrency.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum CryptoErrorCode {

    CRYPTO_DOES_NOT_EXIST(5000, "Crypto does not exist", HttpStatus.BAD_REQUEST),
    INVALID_CRYPTO(5001, "Invalid crypto", HttpStatus.BAD_REQUEST),

    ;

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}