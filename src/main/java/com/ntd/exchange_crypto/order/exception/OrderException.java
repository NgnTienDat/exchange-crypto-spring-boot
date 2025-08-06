package com.ntd.exchange_crypto.order.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderException extends RuntimeException {
    private OrderErrorCode errorCode;
    public OrderException(OrderErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
