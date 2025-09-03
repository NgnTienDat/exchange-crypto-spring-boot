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
@ToString
public class VerificationRequest {
    @NotBlank(message = "NOT_BLANK")
    String userId;

    @NotBlank(message = "NOT_BLANK")
    String code;

    @NotBlank(message = "NOT_BLANK")
    String deviceId;
}
