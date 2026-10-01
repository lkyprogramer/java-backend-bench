package com.orderdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.domain.StockLevel;
import com.orderdesk.repository.StockLevelRepository;

/** Stock bookkeeping: available = onHand - reserved. */
@Service
public class InventoryService {

    private final StockLevelRepository stock;

    public InventoryService(StockLevelRepository stock) {
        this.stock = stock;
    }

    private StockLevel load(String sku) {
        return stock.lockBySku(sku).orElseThrow(() -> BusinessException.notFound("stock", sku));
    }

    @Transactional
    public StockLevel restock(String sku, int quantity) {
        StockLevel level = load(sku);
        level.setOnHand(level.getOnHand() + quantity);
        return level;
    }

    @Transactional
    public void reserve(String sku, int quantity) {
        StockLevel level = load(sku);
        if (level.getAvailable() < quantity) {
            throw new InsufficientStockException(sku, quantity, level.getAvailable());
        }
        level.setReserved(level.getReserved() + quantity);
    }

    @Transactional
    public void release(String sku, int quantity) {
        StockLevel level = load(sku);
        level.setReserved(level.getReserved() - quantity);
    }

    /** Reserved units leave the warehouse. */
    @Transactional
    public void ship(String sku, int quantity) {
        StockLevel level = load(sku);
        level.setReserved(level.getReserved() - quantity);
        level.setOnHand(level.getOnHand() - quantity);
    }
}
