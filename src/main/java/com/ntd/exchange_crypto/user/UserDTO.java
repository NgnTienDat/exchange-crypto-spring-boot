package com.ntd.exchange_crypto.user;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class UserDTO {
    String id;
    String email;
}
