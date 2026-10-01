package com.orderdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderdesk.domain.StockLevel;

public interface StockLevelRepository extends JpaRepository<StockLevel, String> {
}
