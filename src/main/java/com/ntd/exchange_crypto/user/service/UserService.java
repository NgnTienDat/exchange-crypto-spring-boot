package com.ntd.exchange_crypto.user.service;


import com.ntd.exchange_crypto.common.PagedResponse;
import com.ntd.exchange_crypto.user.UserDTO;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import com.ntd.exchange_crypto.user.UserInternalAPI;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.request.UserUpdateRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import com.ntd.exchange_crypto.user.enums.Role;
import com.ntd.exchange_crypto.user.exception.UserErrorCode;
import com.ntd.exchange_crypto.user.exception.UserException;
import com.ntd.exchange_crypto.user.mapper.UserMapper;
import com.ntd.exchange_crypto.user.model.User;
import com.ntd.exchange_crypto.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService implements UserExternalAPI, UserInternalAPI {
    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public UserResponse createUser(UserCreationRequest userCreationRequest) {
        User user = userMapper.toUser(userCreationRequest);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            HashSet<String> roles = new HashSet<>();
            roles.add(Role.USER.name());
            user.setRoles(roles);
            userRepository.save(user);

        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.USER_ALREADY_EXISTS);
        }
        return userMapper.toUserResponse(user);
    }

    @Override
//    @PostAuthorize("returnObject.email == authentication.name")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(String id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOTFOUND)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> pageResult = userRepository.findAll(pageable);
        return new PagedResponse<>(
                pageResult.getContent().stream().map(userMapper::toUserResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );


    }


    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException(UserErrorCode.USER_NOTFOUND);
        }
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserDTO getUserLogin() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException(UserErrorCode.USER_NOTFOUND);
        }

        return UserDTO.builder().id(user.getId()).email(user.getEmail()).build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException(UserErrorCode.USER_NOTFOUND));
        userRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserResponse lockAndUnlockUser(UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(userUpdateRequest.getUserId()).orElseThrow(() -> new UserException(UserErrorCode.USER_NOTFOUND));
        user.setActive(userUpdateRequest.isActive());
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public Long countTotalUsers() {
        return userRepository.countTotalUsers();
    }


    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }
}