package com.orderdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderdesk.domain.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
}
