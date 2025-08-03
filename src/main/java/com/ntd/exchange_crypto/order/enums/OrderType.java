package com.ntd.exchange_crypto.order.enums;

public enum OrderType {
    LIMIT, // Limit order
    MARKET, // Market order
    STOP_LOSS, // Stop-loss order
    STOP_LOSS_LIMIT, // Stop-loss limit order
    TAKE_PROFIT, // Take-profit order
    TAKE_PROFIT_LIMIT, // Take-profit limit order
    TRAILING_STOP // Trailing stop order
}
