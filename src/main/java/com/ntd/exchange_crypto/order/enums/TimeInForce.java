package com.ntd.exchange_crypto.order.enums;

public enum TimeInForce {
    GTC, // Good 'Til Canceled
    IOC, // Immediate Or Cancel
    FOK, // Fill Or Kill
    GTX, // Good 'Til Crossing
    PO, // Post Only
    AON; // All Or None

    @Override
    public String toString() {
        return name();
    }
}
