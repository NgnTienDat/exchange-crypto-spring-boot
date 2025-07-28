package com.ntd.exchange_crypto.auth;

import com.ntd.exchange_crypto.auth.dto.request.AuthenticationRequest;
import com.ntd.exchange_crypto.auth.dto.request.IntrospectRequest;
import com.ntd.exchange_crypto.auth.dto.request.LogoutRequest;
import com.ntd.exchange_crypto.auth.dto.request.RefreshRequest;
import com.ntd.exchange_crypto.auth.dto.request.VerificationRequest;
import com.ntd.exchange_crypto.auth.dto.response.AuthenticationResponse;
import com.ntd.exchange_crypto.auth.dto.response.IntrospectResponse;
import com.ntd.exchange_crypto.auth.dto.response.TFAResponse;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationExternalAPI {

    AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest);

    TFAResponse enableTwoFactorAuthentication();

    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;

    AuthenticationResponse refreshToken(RefreshRequest refreshRequest) throws ParseException, JOSEException;

    void logout(LogoutRequest logoutRequest) throws ParseException, JOSEException;

    AuthenticationResponse verifyCode(VerificationRequest verificationRequest);
}