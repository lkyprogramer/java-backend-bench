package com.orderdesk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.orderdesk.support.ApiTestSupport;

class ProductApiTest extends ApiTestSupport {

    @Test
    void createsProductWithStock() {
        String sku = product(unique("p"), "19.90", 7);
        var view = stock(sku);
        assertThat(view.get("onHand").asInt()).isEqualTo(7);
        assertThat(view.get("available").asInt()).isEqualTo(7);
        assertThat(view.get("price").decimalValue()).isEqualByComparingTo("19.90");
    }

    @Test
    void rejectsDuplicateSku() {
        String sku = product(unique("p"), "1.00", 1);
        var r = call("POST", "/api/products", Map.of("sku", sku, "name", "x", "price", 2, "initialStock", 1));
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.body().at("/error/code").asText()).isEqualTo("PRODUCT_EXISTS");
    }

    @Test
    void restockAddsToOnHand() {
        String sku = product(unique("p"), "1.00", 2);
        var r = call("POST", "/api/inventory/" + sku + "/restock", Map.of("quantity", 5));
        assertThat(r.status()).isEqualTo(200);
        assertThat(stock(sku).get("onHand").asInt()).isEqualTo(7);
    }
}
