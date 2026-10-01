package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** B1: placing an order is all-or-nothing, whatever the reason the order is rejected. */
class HiddenB1AtomicOrderTest extends HiddenSupport {

    @Test
    void insufficientLaterLineLeavesEarlierStockUntouched() {
        String a = product(unique("a"), "1.00", 5);
        String b = product(unique("b"), "1.00", 1);
        String customer = unique("c");
        var r = order(customer, a, 2, b, 3);
        assertThat(r.status()).isEqualTo(409);
        assertThat(stock(a).get("reserved").asInt()).isZero();
        assertThat(ordersOf(customer)).isEmpty();
    }

    @Test
    void unknownLaterSkuLeavesEarlierStockUntouched() {
        String a = product(unique("a"), "1.00", 5);
        String customer = unique("c");
        var r = order(customer, a, 2, unique("zz-missing"), 1);
        assertThat(r.status()).isEqualTo(404);
        assertThat(stock(a).get("reserved").asInt()).isZero();
        assertThat(ordersOf(customer)).isEmpty();
    }

    @Test
    void orderOverLimitLeavesAllStockUntouched() {
        String a = product(unique("a"), "4000.00", 5);
        String b = product(unique("b"), "3000.00", 5);
        String customer = unique("c");
        var r = order(customer, a, 2, b, 1);
        assertThat(r.status()).isEqualTo(422);
        assertThat(stock(a).get("reserved").asInt()).isZero();
        assertThat(stock(b).get("reserved").asInt()).isZero();
        assertThat(ordersOf(customer)).isEmpty();
    }
}
