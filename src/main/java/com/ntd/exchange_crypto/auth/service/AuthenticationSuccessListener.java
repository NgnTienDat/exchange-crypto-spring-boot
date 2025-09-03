package com.ntd.exchange_crypto.auth.service;

import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final RedisTemplate<String, String> redisTemplate;

    public AuthenticationSuccessListener(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();

        String failKey = "login-fail:" + email;
        String lockKey = "login-lock:" + email;

        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);
    }
}
