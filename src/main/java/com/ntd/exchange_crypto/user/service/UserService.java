package com.ntd.exchange_crypto.user.service;
//
//import com.ntd.exchange_crypto.auth.service.TwoFactorAuthenticationService;
//import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
//import com.ntd.exchange_crypto.user.dto.response.UserResponse;
//import com.ntd.exchange_crypto.user.enums.Role;
//import com.ntd.exchange_crypto.user.exception.UserErrorCode;
//import com.ntd.exchange_crypto.user.exception.UserException;
//import com.ntd.exchange_crypto.user.mapper.UserMapper;
//import com.ntd.exchange_crypto.user.model.User;
//import com.ntd.exchange_crypto.user.repository.UserRepository;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.security.access.prepost.PostAuthorize;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.util.HashSet;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class UserService {
//    UserRepository userRepository;
//    UserMapper userMapper;
//
//    public User createUser(UserCreationRequest userCreationRequest) {
//
//        User user = userMapper.toUser(userCreationRequest);
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
//
//
//
//        try {
//            HashSet<String> roles = new HashSet<>();
//            roles.add(Role.USER.name());
//            user.setRoles(roles);
//            userRepository.save(user);
//        } catch (DataIntegrityViolationException e) {
//            throw new UserException(UserErrorCode.USER_ALREADY_EXISTS);
//        }
//        return user;
//    }
//
//    @PostAuthorize("returnObject.email == authentication.name")
//    public UserResponse getUserById(String id) {
//        return userMapper.toUserResponse(userRepository.findById(id)
//                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOTFOUND)));
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    public List<UserResponse> getAllUsers() {
//        return userRepository.findAll()
//                .stream().map(userMapper::toUserResponse).toList();
//    }
//
//    public UserResponse getMyInfo() {
//        var context = SecurityContextHolder.getContext();
//        String email = context.getAuthentication().getName();
//        User user = userRepository.findByEmail(email);
//        if (user == null) {
//            throw new UserException(UserErrorCode.USER_NOTFOUND);
//        }
//        return userMapper.toUserResponse(user);
//    }
//
//    public User getUserByEmail(String email) {
//        return userRepository.findByEmail(email);
//    }
//
//    public void saveUser(User user) {
//        userRepository.save(user);
//    }
//}

import com.ntd.exchange_crypto.user.UserExternalAPI;
import com.ntd.exchange_crypto.user.UserInternalAPI;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
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
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

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
    @PostAuthorize("returnObject.email == authentication.name")
    public UserResponse getUserById(String id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOTFOUND)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
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
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }
}