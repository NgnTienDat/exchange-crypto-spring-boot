package com.ntd.exchange_crypto.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderStatResponse {
    private Long totalOrder;
    private Long activeOrder;
    private Long completeTrades;
}
