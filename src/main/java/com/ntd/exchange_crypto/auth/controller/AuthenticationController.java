package com.ntd.exchange_crypto.auth.controller;

import com.nimbusds.jose.JOSEException;
import com.ntd.exchange_crypto.auth.dto.request.*;
import com.ntd.exchange_crypto.auth.dto.response.AuthenticationResponse;
import com.ntd.exchange_crypto.auth.dto.response.IntrospectResponse;
import com.ntd.exchange_crypto.auth.dto.response.TFAResponse;
import com.ntd.exchange_crypto.auth.service.AuthenticationService;
import com.ntd.exchange_crypto.common.MailServiceExternalApi;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    MailServiceExternalApi mailService;

    // login
    @PostMapping("/login")
    public ResponseEntity<APIResponse<?>> authenticate(
            @RequestBody @Valid AuthenticationRequest authenticationRequest,
            HttpServletRequest request) {

        String ipAddress = authenticationService.getClientIpAddress(request);
        authenticationRequest.setIpAddress(ipAddress);


        AuthenticationResponse result = authenticationService.authenticate(authenticationRequest);

        if (result.getCondition()!=null && result.getCondition().equals("2FA_REQUIRED")) {
            APIResponse<AuthenticationResponse> response = new APIResponse<>(
                    true,
                    HttpStatus.PROCESSING.value(),
                    "2FA required",
                    result
            );

            log.info("Result: {}", result);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        APIResponse<AuthenticationResponse> response = new APIResponse<>(
                true,
                HttpStatus.OK.value(),
                "is authenticated",
                result
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PostMapping("/introspect")
    public ResponseEntity<APIResponse<IntrospectResponse>> introspect(
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
    public ResponseEntity<APIResponse<AuthenticationResponse>> refreshToken(
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



    @PostMapping("/send-otp")
    public ResponseEntity<APIResponse<String>> sendOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        log.info("Sending OTP to email: {}", email);

        mailService.sendOtp(email);
        APIResponse<String> response = new APIResponse<>(
                true,
                HttpStatus.OK.value(),
                "OTP sent to " + email,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<APIResponse<String>> verifyOtp(@RequestBody @Valid Map<String, String> req) {
        String email = req.get("email");
        String code = req.get("code");
        boolean verified = mailService.verifyOtp(email, code);
        if (verified) {
            APIResponse<String> response = new APIResponse<>(
                    true,
                    HttpStatus.OK.value(),
                    "OTP verified successfully",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            APIResponse<String> response = new APIResponse<>(
                    false,
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid or expired OTP",
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }


}














