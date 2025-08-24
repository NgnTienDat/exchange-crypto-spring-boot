package com.ntd.exchange_crypto.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AuthenticationRequest {
    @NotBlank(message = "NOT_BLANK")
    String email;
    @NotBlank(message = "NOT_BLANK")
    String password;
    @NotBlank(message = "NOT_BLANK")
    String deviceId;
    @NotBlank(message = "NOT_BLANK")
    String userAgent;

    String ipAddress;
}
