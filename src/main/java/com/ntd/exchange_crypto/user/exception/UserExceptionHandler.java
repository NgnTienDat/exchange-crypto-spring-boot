package com.ntd.exchange_crypto.user.exception;

import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.ntd.exchange_crypto.user")
@Slf4j
@Order(1)
public class UserExceptionHandler {

    @ExceptionHandler(value = UserException.class)
    public ResponseEntity<APIResponse<Void>> handleUserException(UserException exception) {
        log.info("User exception: ", exception);

        UserErrorCode errorCode = exception.getErrorCode();

        APIResponse<Void> apiResponse = new APIResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }


}
