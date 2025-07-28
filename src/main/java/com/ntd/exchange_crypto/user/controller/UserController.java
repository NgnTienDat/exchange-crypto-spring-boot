package com.ntd.exchange_crypto.user.controller;
//
//import com.ntd.exchange_crypto.common.dto.response.APIResponse;
//import com.ntd.exchange_crypto.user.UserExternalAPI;
//import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
//import com.ntd.exchange_crypto.user.dto.response.UserResponse;
//import com.ntd.exchange_crypto.user.model.User;
//import com.ntd.exchange_crypto.user.service.UserService;
//import jakarta.validation.Valid;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/users")
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class UserController {
//
//    UserExternalAPI userService;
//
//    @PostMapping("/")
//    public ResponseEntity<APIResponse<User>> createUser(@RequestBody @Valid UserCreationRequest userRequest) {
//        UserResponse user = userService.createUser(userRequest);
//
//        APIResponse<User> response = APIResponse.<User>builder()
//                .code(HttpStatus.CREATED.value())
//                .message("User created")
//                .success(true)
//                .build();
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @GetMapping("/{userId}")
//    public ResponseEntity<APIResponse<UserResponse>> getUserById(@PathVariable String userId) {
//        APIResponse<UserResponse> apiResponse = APIResponse.<UserResponse>builder()
//                .success(true)
//                .code(HttpStatus.OK.value())
//                .message("User data retrieved successfully")
//                .result(userService.getUserById(userId))
//                .build();
//
//        return ResponseEntity.ok(apiResponse);
//    }
//
//
//    @GetMapping("/")
//    public ResponseEntity<APIResponse<List<UserResponse>>> getAllUsers() {
//        APIResponse<List<UserResponse>> apiResponse = APIResponse.<List<UserResponse>>builder()
//                .success(true)
//                .code(HttpStatus.OK.value())
//                .message("Users data retrieved successfully")
//                .result(userService.getAllUsers())
//                .build();
//
//        return ResponseEntity.ok(apiResponse);
//    }
//
//
//    @GetMapping("/my-info")
//    public ResponseEntity<APIResponse<UserResponse>> getMyInfo() {
//        APIResponse<UserResponse> response = APIResponse.<UserResponse>builder()
//                .success(true)
//                .code(HttpStatus.OK.value())
//                .message("My Information")
//                .result(this.userService.getMyInfo())
//                .build();
//
//        return ResponseEntity.ok(response);
//    }
//
//
//}

import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserExternalAPI userExternalAPI;

    private <T> APIResponse<T> buildResponse(T result, String message, HttpStatus status) {
        return APIResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message(message)
                .result(result)
                .build();
    }

    @PostMapping("/")
    public ResponseEntity<APIResponse<UserResponse>> createUser(@RequestBody @Valid UserCreationRequest userRequest) {
        UserResponse userResponse = userExternalAPI.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildResponse(userResponse, "User created", HttpStatus.CREATED));
    }

    @GetMapping("/{userId}")
    @PostAuthorize("returnObject.result.email == authentication.name")
    public ResponseEntity<APIResponse<UserResponse>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(buildResponse(userExternalAPI.getUserById(userId),
                "User data retrieved successfully", HttpStatus.OK));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(buildResponse(userExternalAPI.getAllUsers(),
                "Users data retrieved successfully", HttpStatus.OK));
    }

    @GetMapping("/my-info")
    public ResponseEntity<APIResponse<UserResponse>> getMyInfo() {
        return ResponseEntity.ok(buildResponse(userExternalAPI.getMyInfo(),
                "My Information", HttpStatus.OK));
    }
}