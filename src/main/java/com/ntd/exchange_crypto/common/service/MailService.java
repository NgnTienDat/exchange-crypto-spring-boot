package com.ntd.exchange_crypto.common.service;

import com.ntd.exchange_crypto.common.MailServiceExternalApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MailService implements MailServiceExternalApi {
    @Autowired
    private JavaMailSender mailSender;

    private final RedisTemplate<String, Object> redisTemplate;

    private final Random random = new Random();


    @Override
    public void sendOtp(String email) {

        String otp = String.valueOf(100000 + random.nextInt(900000));

        redisTemplate.opsForValue().set("otp:" + email, otp, 10, TimeUnit.MINUTES);


        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("dat.nt334@gmail.com");
        msg.setTo(email);
        msg.setSubject("[CryptoCoin] Confirm Your Registration");
        msg.setText("Your verification code is: \n" + otp + "\n (expires in 5 minutes)");
        mailSender.send(msg);
    }

    @Override
    public boolean verifyOtp(String email, String code) {
        String key = "otp:" + email;
        String savedOtp = (String) redisTemplate.opsForValue().get(key);
        if (savedOtp != null && savedOtp.equals(code)) {
            // OTP đúng → đánh dấu verified
            redisTemplate.opsForValue().set("verified:" + email, "true", 15, TimeUnit.MINUTES);
            redisTemplate.delete(key); // xoá OTP
            return true;
        }
        return false;
    }

    @Override
    public boolean isVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get("verified:" + email));
    }

    @Override
    public void clearVerified(String email) {
        redisTemplate.delete("verified:" + email);
    }

    @Override
    public void sendLoginAlert(String email, String ipAddress, String deviceName) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("dat.nt334@gmail.com");
        msg.setTo(email);
        msg.setSubject("[CryptoCoin] New Device or IP Login Alert");

        String text = """
        New Device or IP Login Detected on Your Account

        We detected a login to your account %s from a new device or IP address. 
        If this was not you, please change your password or temporarily disable your account immediately.

        Time : %s (UTC)
        Device : %s
        IP Address : %s
        Location : Ho Chi Minh City Vietnam
        """.formatted(email, java.time.Instant.now(), deviceName, ipAddress);

        msg.setText(text);
        mailSender.send(msg);
    }


}
