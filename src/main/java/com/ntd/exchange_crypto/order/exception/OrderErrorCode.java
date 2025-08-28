package com.ntd.exchange_crypto.order.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum OrderErrorCode {

    INSUFFICIENT_BALANCE(5001, "Insufficient balance", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(5002, "Order not found", HttpStatus.BAD_REQUEST),
    INVALID_MONTH(5003, "Month must be between 1 and 12", HttpStatus.BAD_REQUEST),
    INVALID_YEAR(5004, "Year must be between 1970 and 2100", HttpStatus.BAD_REQUEST)
    ;

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}