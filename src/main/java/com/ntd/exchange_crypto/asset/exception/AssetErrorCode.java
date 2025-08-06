package com.ntd.exchange_crypto.asset.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AssetErrorCode {

    USER_ASSET_NOTFOUND(4000, "Asset not found", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(4001, "Insufficient balance", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE_TO_LOCK(4002, "Insufficient balance to lock quantity", HttpStatus.BAD_REQUEST),
    LOCKED_BALANCE_INSUFFICIENT(4003, "locked balance insufficient to unlock", HttpStatus.BAD_REQUEST),
    ;

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}