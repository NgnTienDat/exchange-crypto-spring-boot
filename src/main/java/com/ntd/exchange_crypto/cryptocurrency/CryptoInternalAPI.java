package com.ntd.exchange_crypto.cryptocurrency;


public interface CryptoInternalAPI {
    /**
     * Cập nhật thông tin crypto từ nguồn bên ngoài (ví dụ: API Binance).
     *
     * @param productId Định danh crypto
     * @param totalSupply Tổng cung mới
     * @param price Giá hiện tại (tùy chọn)
     * @return Crypto đã cập nhật
     */
//    Optional<Crypto> updateCryptoData(String productId, BigDecimal totalSupply, BigDecimal price);

    /**
     * Thêm một loại crypto mới vào hệ thống.
     *
     * @param productId Định danh crypto
     * @param name Tên đầy đủ
     * @param symbol Biểu tượng
     * @return Crypto đã tạo
     */
//    Crypto createCrypto(String productId, String name, String symbol);

    /**
     * Xóa một loại crypto (nếu không còn sử dụng).
     *
     * @param productId Định danh crypto
     * @return true nếu xóa thành công
     */
    boolean deleteCrypto(String productId);
}
