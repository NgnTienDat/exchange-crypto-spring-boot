package com.ntd.exchange_crypto.asset.exception;

import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ntd.exchange_crypto.user")
@Slf4j
@Order(1)
public class AssetExceptionHandler {

    @ExceptionHandler(value = AssetException.class)
    public ResponseEntity<APIResponse<Void>> handleAssetException(AssetException exception) {
        log.info("Asset exception: ", exception);

        AssetErrorCode errorCode = exception.getErrorCode();

        APIResponse<Void> apiResponse = new APIResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }


}
