package com.ntd.exchange_crypto.user.controller;

import com.ntd.exchange_crypto.common.PagedResponse;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<UserResponse>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(buildResponse(userExternalAPI.getUserById(userId),
                "User data retrieved successfully", HttpStatus.OK));
    }

//    @GetMapping("/")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<APIResponse<List<UserResponse>>> getAllUsers() {
//        return ResponseEntity.ok(buildResponse(userExternalAPI.getAllUsers(),
//                "Users data retrieved successfully", HttpStatus.OK));
//    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<UserResponse> users = userExternalAPI.getAllUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(buildResponse(
                users,
                "Users data retrieved successfully",
                HttpStatus.OK
        ));
    }



    @GetMapping("/my-info")
    public ResponseEntity<APIResponse<UserResponse>> getMyInfo() {
        return ResponseEntity.ok(buildResponse(userExternalAPI.getMyInfo(),
                "My Information", HttpStatus.OK));
    }
}