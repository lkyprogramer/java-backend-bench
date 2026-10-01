package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;

/** Concurrent partial shipments of one order never ship more than was ordered. */
class HiddenT3ShipmentConcurrencyTest extends HiddenT3MutableSupport {

    @RepeatedTest(2)
    void concurrentShipmentsNeverOverShip() throws Exception {
        String a = product(unique("a"), "1.00", 50);
        long id = paidOrder(a, 6);
        int n = 24;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CyclicBarrier start = new CyclicBarrier(n);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return ship(id, a, 1).status();
            }));
        }
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get());
        }
        pool.shutdown();
        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(6);
        assertThat(statuses).allMatch(s -> s == 201 || s == 409 || s == 422);
        assertThat(stock(a).get("onHand").asInt()).isEqualTo(44);
        assertThat(stock(a).get("reserved").asInt()).isZero();
        assertThat(v2(id).get("status").asText()).isEqualTo("SHIPPED");
    }
}
