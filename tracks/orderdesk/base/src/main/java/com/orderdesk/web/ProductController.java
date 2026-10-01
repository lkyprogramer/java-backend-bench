package com.orderdesk.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orderdesk.service.CatalogService;
import com.orderdesk.service.InventoryService;
import com.orderdesk.web.dto.ProductView;
import com.orderdesk.web.dto.Requests;
import com.orderdesk.web.dto.StockView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final CatalogService catalog;
    private final InventoryService inventory;

    public ProductController(CatalogService catalog, InventoryService inventory) {
        this.catalog = catalog;
        this.inventory = inventory;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody Requests.CreateProduct request) {
        return catalog.create(request.sku(), request.name(), request.price(), request.initialStock());
    }

    @GetMapping("/products/{sku}")
    public ProductView get(@PathVariable String sku) {
        return catalog.get(sku);
    }

    @PostMapping("/inventory/{sku}/restock")
    public StockView restock(@PathVariable String sku, @Valid @RequestBody Requests.Restock request) {
        return StockView.of(inventory.restock(sku, request.quantity()));
    }
}
