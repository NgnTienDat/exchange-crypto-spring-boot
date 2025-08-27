package com.ntd.exchange_crypto.trade.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeException extends RuntimeException {
    private TradeErrorCode errorCode;
    public TradeException(TradeErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
