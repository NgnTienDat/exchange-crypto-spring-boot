package com.ntd.exchange_crypto.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AuthErrorCode {
    UNCATEGORIZED_ERROR(666, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_EXISTS(2002, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2003, "You dont have permission", HttpStatus.FORBIDDEN),
    QR_GENERATED_FAILED(2004, "You dont have permission", HttpStatus.BAD_REQUEST),
    INVALID_CODE(2004, "Invalid verification code", HttpStatus.BAD_REQUEST),
    ;

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}