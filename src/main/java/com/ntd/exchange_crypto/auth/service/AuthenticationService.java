package com.ntd.exchange_crypto.auth.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ntd.exchange_crypto.auth.AuthenticationExternalAPI;
import com.ntd.exchange_crypto.auth.dto.request.*;
import com.ntd.exchange_crypto.auth.dto.response.AuthenticationResponse;
import com.ntd.exchange_crypto.auth.dto.response.IntrospectResponse;
import com.ntd.exchange_crypto.auth.dto.response.TFAResponse;
import com.ntd.exchange_crypto.auth.exception.AuthErrorCode;
import com.ntd.exchange_crypto.auth.exception.AuthException;
import com.ntd.exchange_crypto.auth.model.InvalidatedToken;
import com.ntd.exchange_crypto.auth.model.TrustedDevice;
import com.ntd.exchange_crypto.auth.repository.AuthenticationRepository;
import com.ntd.exchange_crypto.auth.repository.InvalidatedTokenRepository;
import com.ntd.exchange_crypto.auth.repository.httpClient.OutboundIdentityClient;
import com.ntd.exchange_crypto.auth.repository.TrustedDeviceRepository;
import com.ntd.exchange_crypto.auth.repository.httpClient.OutboundUserClient;
import com.ntd.exchange_crypto.common.MailServiceExternalApi;
import com.ntd.exchange_crypto.user.enums.Role;
import com.ntd.exchange_crypto.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService implements AuthenticationExternalAPI {

    private final AuthenticationRepository authenticationRepository;
    private final InvalidatedTokenRepository tokenRepository;
    private final TwoFactorAuthenticationService tfaService;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final OutboundIdentityClient outboundIdentityClient;
    private final OutboundUserClient outboundUserClient;
    private final MailServiceExternalApi mailService;
    private final RedisTemplate<String, String> redisTemplate;
    private ApplicationEventPublisher eventPublisher;

    @NonFinal
    @Value("${auth.signer-key}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${auth.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${auth.refreshable-duration}")
    protected long REFRESH_DURATION;


    @NonFinal
    @Value("${external.api.identity.client-id}")
    protected String CLIENT_ID;

    @NonFinal
    @Value("${external.api.identity.client-secret}")
    protected String CLIENT_SECRET;

    @NonFinal
    @Value("${external.api.identity.redirect-uri}")
    protected String REDIRECT_URI;

    @NonFinal
    protected String GRANT_TYPE = "authorization_code";


    /**
     * Login:
     * -> Check user is enabled 2FA or not
     * -> if not enabled, return token
     * -> if enabled, check if device is trusted (verified == true) -> return token
     * -> if not trusted, user enter code from authenticator app -> verify code -> return token
     */

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {

        if (Boolean.TRUE.equals(redisTemplate.hasKey("login-lock" + authenticationRequest.getEmail()))) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
        }


        User user = authenticationRepository
                .findByEmail(authenticationRequest.getEmail())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_EXISTS));

        if (!user.isActive()) throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean isAuthenticated = passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword());

        if (!isAuthenticated) {
            eventPublisher.publishEvent(
                    new AuthenticationFailureBadCredentialsEvent(
                            new UsernamePasswordAuthenticationToken(
                                    authenticationRequest.getEmail(),
                                    authenticationRequest.getPassword()
                            ),
                            new BadCredentialsException("Invalid password")
                    )
            );
            throw new AuthException(AuthErrorCode.UNAUTHENTICATED);
        }

        TrustedDevice device = trustedDeviceRepository
                .findByDeviceIdAndUserId(authenticationRequest.getDeviceId(), user.getId())
                .orElseGet(() -> trustedDeviceRepository.save(
                        TrustedDevice.builder()
                                .userId(user.getId())
                                .deviceId(authenticationRequest.getDeviceId())
                                .userAgent(authenticationRequest.getUserAgent())
                                .ipAddress(authenticationRequest.getIpAddress())
                                .verified(false)
                                .build()
                ));

        if (!user.isTfaEnabled()) {
            String token = generateToken(user);
            return AuthenticationResponse.builder()
                    .roles(user.getRoles())
                    .token(token)
                    .isAuthenticated(true)
                    .build();
        }

        if (!device.isVerified()) {
            return AuthenticationResponse.builder()
                    .userId(user.getId())
                    .condition("2FA_REQUIRED")
                    .build();
        }

        String token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .isAuthenticated(true)
                .condition("SUCCESS")
                .roles(user.getRoles())
                .build();
    }


    @Override
    @Transactional
    public AuthenticationResponse outboundAuthenticate(String code) {
        var response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .grantType(GRANT_TYPE)
                .build());

        var userInfo = outboundUserClient.getUserInfo("json", response.getAccessToken());

        HashSet<String> roles = new HashSet<>();
        roles.add(Role.USER.name());

        User user = authenticationRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> {
                    try {
                        return authenticationRepository.save(User.builder()
                                .email(userInfo.getEmail())
                                .fullName(userInfo.getName())
                                .avatar(userInfo.getPicture())
                                .roles(roles)
                                .build());
                    } catch (DataIntegrityViolationException ex) {

                        return authenticationRepository.findByEmail(userInfo.getEmail())
                                .orElseThrow();
                    }
                });

        String token = generateToken(user);
        return AuthenticationResponse.builder().token(token).build();
    }



    @Override
    public TFAResponse enableTwoFactorAuthentication() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        User user = authenticationRepository
                .findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_EXISTS));

        if (!user.isTfaEnabled()) {
            user.setSecret(tfaService.generateNewSecret());
        }

        authenticationRepository.save(user);
        return TFAResponse.builder()
                .secretImageUri(tfaService.generateQrCodeImageUri(user.getSecret()))
                .tfaEnabled(true)
                .build();
    }


    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> stringJoiner.add("ROLE_" + role));
        }
        return stringJoiner.toString();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("ntd.exchange_crypto.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create JWT token", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        String token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (AuthException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // Sửa lỗi vòng lặp refresh token bằng cách kiểm tra thời gian hợp lệ
    @Override
    public AuthenticationResponse refreshToken(RefreshRequest refreshRequest) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyToken(refreshRequest.getToken(), true);

        String jit = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // Kiểm tra xem token đã được invalidate trước đó chưa
        if (tokenRepository.existsById(jit)) {
            throw new AuthException(AuthErrorCode.UNAUTHENTICATED);
        }

        String email = signedJWT.getJWTClaimsSet().getSubject();
        User user = authenticationRepository
                .findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHENTICATED));

        String newToken = generateToken(user);
        InvalidatedToken invalidatedToken = new InvalidatedToken(jit, expiryTime);
        tokenRepository.save(invalidatedToken);

        return AuthenticationResponse.builder()
                .token(newToken)
                .isAuthenticated(true)
                .build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiration = isRefresh
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant().plus(REFRESH_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        if (!(signedJWT.verify(verifier) && expiration.after(new Date()))) {
            throw new AuthException(AuthErrorCode.UNAUTHENTICATED);
        }

        if (tokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AuthException(AuthErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    @Override
    public void logout(LogoutRequest logoutRequest) throws ParseException, JOSEException {
        try {
            SignedJWT signedToken = verifyToken(logoutRequest.getToken(), true);
            String jit = signedToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();
            InvalidatedToken invalidatedToken = new InvalidatedToken(jit, expiryTime);
            tokenRepository.save(invalidatedToken);
        } catch (AuthException e) {
            log.info("Token is expired or invalid during logout");
        }
    }

    @Override
    public AuthenticationResponse verifyCode(VerificationRequest verificationRequest) {

        User user = authenticationRepository
                .findById(verificationRequest.getUserId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_EXISTS));

        log.info("user: {}", user.getEmail());

        if (tfaService.isOtpNotValid(user.getSecret(), verificationRequest.getCode())) {
            throw new AuthException(AuthErrorCode.INVALID_CODE);
        }

        user.setTfaEnabled(true);
        authenticationRepository.save(user);

        TrustedDevice device = trustedDeviceRepository
                .findByDeviceIdAndUserId(verificationRequest.getDeviceId(), user.getId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.DEVICE_NOT_FOUND));

        device.setVerified(true);
        device.setVerifiedAt(Instant.now());
        trustedDeviceRepository.save(device);

        String token = generateToken(user);

        mailService.sendLoginAlert(user.getEmail(), device.getIpAddress(), device.getUserAgent());

        return AuthenticationResponse.builder()
                .token(token)
                .isAuthenticated(true)
                .build();
    }

    @Override
    @PostAuthorize("returnObject.true == authentication.name")
    public boolean isUserLogin(String emailAuthentication) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();
        return emailAuthentication != null && emailAuthentication.equals(email);
    }

    @Override
    public String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For"); // nếu dùng proxy / load balancer
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr(); // fallback
        }
        return ip;
    }
}