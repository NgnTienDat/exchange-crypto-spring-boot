package com.ntd.exchange_crypto.auth.controller;

import com.nimbusds.jose.JOSEException;
import com.ntd.exchange_crypto.auth.dto.request.*;
import com.ntd.exchange_crypto.auth.dto.response.AuthenticationResponse;
import com.ntd.exchange_crypto.auth.dto.response.IntrospectResponse;
import com.ntd.exchange_crypto.auth.dto.response.TFAResponse;
import com.ntd.exchange_crypto.auth.service.AuthenticationService;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    // login
    @PostMapping("/login")
    public ResponseEntity<APIResponse<?>> authenticate(
            @RequestBody @Valid AuthenticationRequest authenticationRequest) {
        AuthenticationResponse result = authenticationService.authenticate(authenticationRequest);

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
    @PreAuthorize("isAuthenticated()")
    public APIResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {
        this.authenticationService.logout(logoutRequest);
        return APIResponse.<Void>builder()
                .message("Logout")
                .success(true)
                .code(HttpStatus.NO_CONTENT.value())
                .build();
    }


    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
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


    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<TFAResponse>> enable2fa() {
        APIResponse<TFAResponse> response = APIResponse.<TFAResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Enable 2FA")
                .result(authenticationService.enableTwoFactorAuthentication())
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/2fa/verify-code")
    public ResponseEntity<APIResponse<?>> verifyCode(
            @RequestBody @Valid VerificationRequest verificationRequest
    ) {
        APIResponse<AuthenticationResponse> response = APIResponse.<AuthenticationResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Verify code")
                .result(authenticationService.verifyCode(verificationRequest))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}














