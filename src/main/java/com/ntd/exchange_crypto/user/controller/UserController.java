package com.ntd.exchange_crypto.user.controller;

import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import com.ntd.exchange_crypto.user.model.User;
import com.ntd.exchange_crypto.user.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PostMapping("/")
    public ResponseEntity<APIResponse<User>> createUser(@RequestBody @Valid UserCreationRequest userRequest) {
        User user = userService.createUser(userRequest);

        APIResponse<User> response = APIResponse.<User>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created")
                .success(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<APIResponse<UserResponse>> getUserById(@PathVariable String userId) {
        APIResponse<UserResponse> apiResponse = APIResponse.<UserResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("User data retrieved successfully")
                .result(userService.getUserById(userId))
                .build();

        return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/")
    public ResponseEntity<APIResponse<List<UserResponse>>> getAllUsers() {
        APIResponse<List<UserResponse>> apiResponse = APIResponse.<List<UserResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Users data retrieved successfully")
                .result(userService.getAllUsers())
                .build();

        return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/my-info")
    public ResponseEntity<APIResponse<UserResponse>> getMyInfo() {
        APIResponse<UserResponse> response = APIResponse.<UserResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("My Information")
                .result(this.userService.getMyInfo())
                .build();

        return ResponseEntity.ok(response);
    }


}
