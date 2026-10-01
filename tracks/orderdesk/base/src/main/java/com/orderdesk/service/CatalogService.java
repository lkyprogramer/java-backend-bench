package com.orderdesk.service;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.domain.Product;
import com.orderdesk.domain.StockLevel;
import com.orderdesk.repository.ProductRepository;
import com.orderdesk.repository.StockLevelRepository;
import com.orderdesk.web.dto.ProductView;

@Service
public class CatalogService {

    private final ProductRepository products;
    private final StockLevelRepository stock;

    public CatalogService(ProductRepository products, StockLevelRepository stock) {
        this.products = products;
        this.stock = stock;
    }

    @Transactional
    public ProductView create(String sku, String name, BigDecimal price, int initialStock) {
        if (products.existsById(sku)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PRODUCT_EXISTS", "Product " + sku + " already exists");
        }
        Product product = products.save(new Product(sku, name, price));
        StockLevel level = stock.save(new StockLevel(sku, initialStock));
        return ProductView.of(product, level);
    }

    @Transactional(readOnly = true)
    public ProductView get(String sku) {
        Product product = products.findById(sku).orElseThrow(() -> BusinessException.notFound("product", sku));
        StockLevel level = stock.findById(sku).orElseThrow(() -> BusinessException.notFound("stock", sku));
        return ProductView.of(product, level);
    }
}
