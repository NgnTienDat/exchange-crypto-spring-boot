package com.ntd.exchange_crypto.user.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserCreationRequest {


    @Email(message = "INVALID_EMAIL")
    @NotBlank(message = "NOT_BLANK")
//    @Size(max = 255, message = "Email không được quá 100 ký tự!")
    String email;

    @NotBlank(message = "NOT_BLANK")
    @Pattern(regexp = "^(\\+84|0)(3|5|7|8|9)[0-9]{8}$", message = "INVALID_PHONE")
    String phone;


    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&:()])[A-Za-z\\d@$!%*?&:()]{8,}$",
            message = "INVALID_PASSWORD"
    )
    @NotBlank(message = "NOT_BLANK")
    String password;


    @Size(max = 100, message = "Họ không được quá 45 ký tự!")
    @NotBlank(message = "NOT_BLANK")
    String fullName;

    boolean tfaEnabled;
}
