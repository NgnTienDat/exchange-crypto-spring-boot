package com.ntd.exchange_crypto.common;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SliceResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private boolean hasNext;
}
