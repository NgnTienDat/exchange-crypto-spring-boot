package com.ntd.exchange_crypto.user;

import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface UserExternalAPI {
    UserResponse createUser(UserCreationRequest userCreationRequest);

    @PostAuthorize("returnObject.email == authentication.name")
    UserResponse getUserById(String id);

    @PreAuthorize("hasRole('ADMIN')")
    List<UserResponse> getAllUsers();

    UserResponse getMyInfo();

    UserDTO getUserLogin();



}