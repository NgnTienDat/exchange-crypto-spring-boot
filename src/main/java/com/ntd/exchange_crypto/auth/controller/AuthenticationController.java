package com.ntd.exchange_crypto.auth.controller;

import com.nimbusds.jose.JOSEException;
import com.ntd.exchange_crypto.auth.dto.request.AuthenticationRequest;
import com.ntd.exchange_crypto.auth.dto.request.IntrospectRequest;
import com.ntd.exchange_crypto.auth.dto.request.LogoutRequest;
import com.ntd.exchange_crypto.auth.dto.request.RefreshRequest;
import com.ntd.exchange_crypto.auth.dto.response.AuthenticationResponse;
import com.ntd.exchange_crypto.auth.dto.response.IntrospectResponse;
import com.ntd.exchange_crypto.auth.service.AuthenticationService;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;


    // login
    @PostMapping("/login")
    public ResponseEntity<APIResponse<AuthenticationResponse>> authenticate(
            @RequestBody @Valid AuthenticationRequest authenticationRequest) {
        AuthenticationResponse result = authenticationService.authenticated(authenticationRequest);
        APIResponse<AuthenticationResponse> response = new APIResponse<>(
                true,
                HttpStatus.OK.value(),
                "is authenticated",
                result
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PostMapping("/introspect")
    public ResponseEntity<APIResponse<IntrospectResponse>> authenticate(
            @RequestBody @Valid IntrospectRequest introspectRequest) throws ParseException, JOSEException {

        IntrospectResponse result = authenticationService.introspect(introspectRequest);
        APIResponse<IntrospectResponse> response = new APIResponse<>(
                true,
                HttpStatus.OK.value(),
                "Verify token",
                result
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public APIResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {
        this.authenticationService.logout(logoutRequest);
        return APIResponse.<Void>builder()
                .message("Logout")
                .success(true)
                .code(HttpStatus.NO_CONTENT.value())
                .build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<AuthenticationResponse>> authenticate(
            @RequestBody RefreshRequest refreshRequest) throws ParseException, JOSEException {

        AuthenticationResponse result = authenticationService.refreshToken(refreshRequest);
        APIResponse<AuthenticationResponse> response = new APIResponse<>(
                true,
                HttpStatus.OK.value(),
                "Refresh token",
                result
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
