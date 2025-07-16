package com.ntd.exchange_crypto.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum UserErrorCode {

    USER_NOTFOUND(1006, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(1007, "User already exists", HttpStatus.CONFLICT),
    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2003, "You dont have permission", HttpStatus.FORBIDDEN),
    ;

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}