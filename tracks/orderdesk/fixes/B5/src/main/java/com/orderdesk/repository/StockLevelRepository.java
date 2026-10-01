package com.orderdesk.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.orderdesk.domain.StockLevel;

public interface StockLevelRepository extends JpaRepository<StockLevel, String> {

    /** SELECT ... FOR UPDATE: concurrent writers of the same sku queue up instead of losing updates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockLevel s where s.sku = :sku")
    Optional<StockLevel> lockBySku(String sku);
}
