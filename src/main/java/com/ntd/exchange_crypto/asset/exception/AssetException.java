package com.ntd.exchange_crypto.asset.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetException extends RuntimeException {
    private AssetErrorCode errorCode;
    public AssetException(AssetErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
