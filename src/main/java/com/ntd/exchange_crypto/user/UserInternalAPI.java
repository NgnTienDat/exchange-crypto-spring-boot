package com.ntd.exchange_crypto.user;

import com.ntd.exchange_crypto.user.model.User;

public interface UserInternalAPI {
    User getUserByEmail(String email);

    void saveUser(User user);
}