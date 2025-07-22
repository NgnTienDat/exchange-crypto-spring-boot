package com.ntd.exchange_crypto.auth.exception;


import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import dev.samstevens.totp.exceptions.QrGenerationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
@Order(1)
public class AuthExceptionHandler {


    @ExceptionHandler(AuthException.class)
    public ResponseEntity<APIResponse<Void>> handleAuthException(AuthException exception) {

        AuthErrorCode errorCode = exception.getErrorCode();
        APIResponse<Void> response = new APIResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {

        AuthErrorCode errorCode = AuthErrorCode.UNAUTHORIZED;
        APIResponse<Void> response = new APIResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
    }

//    @ExceptionHandler(QrGenerationException.class)
//    public ResponseEntity<APIResponse<Void>> handleQrGenerateException(QrGenerationException exception) {
//
//        AuthErrorCode errorCode = AuthErrorCode.UNAUTHORIZED;
//        APIResponse<Void> response = new APIResponse<>(
//                false,
//                errorCode.getCode(),
//                errorCode.getMessage(),
//                null
//        );
//
//        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
//    }
}
