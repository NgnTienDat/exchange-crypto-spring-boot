package com.ntd.exchange_crypto.user.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserResponse {
    String id;
    String email;
    String fullName;
    String phone;
    String avatar;
    boolean active;
    boolean is2FAEnabled;
    Set<String> roles;

}
