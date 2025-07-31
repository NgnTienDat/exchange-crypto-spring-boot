package com.ntd.exchange_crypto.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED_ERROR(666, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_MESSAGE_KEY(1000, "Invalid Message Key", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1001, "Username must contain at least 8 characters and at most 45 characters", HttpStatus.BAD_REQUEST),
    NOT_BLANK(1002, "Cannot blank this field", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1003, "Invalid email address" , HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Invalid password", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(1005, "Invalid phone number", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1008, "Invalid date of birth", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2003, "You dont have permission", HttpStatus.FORBIDDEN),
//    SUBSCRIBE_FAILED(2004, "You dont have permission", HttpStatus.FORBIDDEN),
    ;

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}