package com.ntd.exchange_crypto.common;

public interface MailServiceExternalApi {
    void sendOtp(String email);

    boolean verifyOtp(String email, String code);

    boolean isVerified(String email);

    void clearVerified(String email);
}
