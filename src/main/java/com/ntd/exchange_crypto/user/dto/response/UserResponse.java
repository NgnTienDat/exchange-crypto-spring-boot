package com.ntd.exchange_crypto.user.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    String id;
    String email;
    String fullName;
    String phone;
    String avatar;
    boolean active;
    boolean tfaEnabled;
    Set<String> roles;
    boolean noPassword;

}
