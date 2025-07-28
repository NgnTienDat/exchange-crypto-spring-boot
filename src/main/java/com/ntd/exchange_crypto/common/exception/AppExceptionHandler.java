package com.ntd.exchange_crypto.common.exception;

import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@Order(2)
public class AppExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<APIResponse> handlingRuntimeException(Exception exception) {
        log.info("App exception: ", exception);
        APIResponse apiResponse = new APIResponse();
        apiResponse.setSuccess(false);
        apiResponse.setCode(ErrorCode.UNCATEGORIZED_ERROR.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_ERROR.getMessage());
        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_ERROR.getHttpStatusCode()).body(apiResponse);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String key = error.getDefaultMessage();

            ErrorCode errorCode;
            try {
                errorCode = ErrorCode.valueOf(key.trim()); // tránh lỗi do khoảng trắng

            } catch (IllegalArgumentException | NullPointerException e) {
                errorCode = ErrorCode.INVALID_MESSAGE_KEY;
            }

            errors.put(error.getField(), errorCode.getMessage());
        }

        APIResponse<Map<String, String>> response = new APIResponse<>(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handleAccessDeniedException(AuthorizationDeniedException exception) {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        APIResponse<Void> response = new APIResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(response);
    }
}
