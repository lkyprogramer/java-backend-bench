package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** B2: a declined charge changes nothing; the order can be paid or cancelled afterwards. */
class HiddenB2PaymentRollbackTest extends HiddenSupport {

    @Test
    void declinedPaymentLeavesOrderUnpaid() {
        String sku = product(unique("p"), "25.00", 5);
        String customer = unique("c");
        long id = order(customer, sku, 2).body().get("id").asLong();
        var declined = pay(id, DECLINED_CARD);
        assertThat(declined.status()).isEqualTo(402);
        assertThat(ordersOf(customer).get(0).get("status").asText()).isEqualTo("PENDING_PAYMENT");
        var cancelled = call("POST", "/api/orders/" + id + "/cancel", null);
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.body().get("paidAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(stock(sku).get("reserved").asInt()).isZero();
    }

    @Test
    void declinedThenAcceptedPaymentCapturesOnce() {
        String sku = product(unique("p"), "25.00", 5);
        long id = order(unique("c"), sku, 2).body().get("id").asLong();
        assertThat(pay(id, DECLINED_CARD).status()).isEqualTo(402);
        var paid = pay(id, GOOD_CARD);
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.body().get("status").asText()).isEqualTo("PAID");
        assertThat(paid.body().get("paidAmount").decimalValue()).isEqualByComparingTo("50.00");
    }
}
