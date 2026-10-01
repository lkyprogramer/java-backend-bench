package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;

/** B5: concurrent stock changes are neither lost nor allowed to oversell. */
class HiddenB5StockConcurrencyTest extends HiddenSupport {

    private <T> List<T> race(int n, java.util.function.IntFunction<Callable<T>> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CyclicBarrier start = new CyclicBarrier(n);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Callable<T> c = task.apply(i);
            futures.add(pool.submit(() -> {
                start.await();
                return c.call();
            }));
        }
        List<T> out = new ArrayList<>();
        for (Future<T> f : futures) {
            out.add(f.get());
        }
        pool.shutdown();
        return out;
    }

    @RepeatedTest(2)
    void concurrentRestocksAreAllCounted() throws Exception {
        String sku = product(unique("p"), "1.00", 0);
        List<Integer> statuses = race(32, i -> () -> call("POST", "/api/inventory/" + sku + "/restock", Map.of("quantity", 1)).status());
        assertThat(statuses).containsOnly(200);
        assertThat(stock(sku).get("onHand").asInt()).isEqualTo(32);
    }

    @RepeatedTest(2)
    void concurrentOrdersNeverOversell() throws Exception {
        String sku = product(unique("p"), "1.00", 8);
        List<Integer> statuses = race(32, i -> () -> order(unique("c"), sku, 1).status());
        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(8);
        assertThat(stock(sku).get("reserved").asInt()).isEqualTo(8);
    }
}
