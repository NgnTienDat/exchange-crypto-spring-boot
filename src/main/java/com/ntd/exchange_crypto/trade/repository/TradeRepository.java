package com.ntd.exchange_crypto.trade.repository;

import com.ntd.exchange_crypto.trade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
}
