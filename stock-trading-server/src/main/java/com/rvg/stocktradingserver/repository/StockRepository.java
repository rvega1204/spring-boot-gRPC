package com.rvg.stocktradingserver.repository;

import com.rvg.stocktradingserver.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing Stock entities.
 */
public interface StockRepository extends JpaRepository<Stock, Long> {
    Stock findByStockSymbol(String stockSymbol);
}
