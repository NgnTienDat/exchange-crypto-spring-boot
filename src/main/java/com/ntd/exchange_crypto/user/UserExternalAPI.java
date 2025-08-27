package com.ntd.exchange_crypto.user;

import com.ntd.exchange_crypto.common.PagedResponse;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.request.UserUpdateRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserExternalAPI {
    UserResponse createUser(UserCreationRequest userCreationRequest);

//    @PreAuthorize("hasRole('ADMIN')")
    UserResponse getUserById(String id);

    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getMyInfo();

    UserDTO getUserLogin();

    @PreAuthorize("hasRole('ADMIN')")
    void deleteUser(String id);

    @PreAuthorize("hasRole('ADMIN')")
    UserResponse lockAndUnlockUser(UserUpdateRequest userUpdateRequest);

}