package com.ntd.exchange_crypto.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class OrderCreationRequest {
    @NotBlank(message = "NOT_BLANK")
    String getCryptoId;

    @NotBlank(message = "NOT_BLANK")
    String giveCryptoId;

//    @NotBlank(message = "NOT_BLANK")
//    String userId;

    @NotBlank(message = "NOT_BLANK")
    String side;

    @NotNull(message = "NOT_NULL")
    BigDecimal price;

    @NotNull(message = "NOT_NULL")
    BigDecimal quantity;

    @NotBlank(message = "NOT_BLANK")
    @Size(max = 10, message = "SIZE_MAX_10")
    String timeInForce;

    @NotBlank(message = "NOT_BLANK")
    String orderStatus;

    @NotBlank(message = "NOT_BLANK")
    String orderType;



    @Override
    public String toString() {
        return getCryptoId + "-" + giveCryptoId;
    }
}
