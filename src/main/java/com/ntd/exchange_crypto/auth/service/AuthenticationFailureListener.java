package com.ntd.exchange_crypto.auth.service;

import com.ntd.exchange_crypto.auth.repository.AuthenticationRepository;
import com.ntd.exchange_crypto.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private final RedisTemplate<String, String> redisTemplate;
    private final AuthenticationRepository authenticationRepository;
    private final int MAX_ATTEMPTS = 5;
    private final long LOCK_TIME_SECONDS = 15 * 60; // 15p

    public AuthenticationFailureListener(RedisTemplate<String, String> redisTemplate,
                                         AuthenticationRepository authenticationRepository) {
        this.redisTemplate = redisTemplate;
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        String keyFail = "login-fail:" + email;
        String keyLock = "login-lock:" + email;

        Long attempts = redisTemplate.opsForValue().increment(keyFail, 1L);

        if (attempts == 1) {
            redisTemplate.expire(keyFail, LOCK_TIME_SECONDS, TimeUnit.SECONDS);
        }

        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(keyLock, "LOCKED", LOCK_TIME_SECONDS, TimeUnit.SECONDS);

            authenticationRepository.findByEmail(email).ifPresent(user -> {
                user.setActive(false);
                log.info("User {} is locked due to too many failed login attempts", email);
                authenticationRepository.save(user);
            });
        }
    }

}
