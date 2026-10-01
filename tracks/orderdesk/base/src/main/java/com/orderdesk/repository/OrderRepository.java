package com.orderdesk.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.orderdesk.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select distinct o from Order o left join fetch o.lines where o.customerId = :customerId order by o.id")
    List<Order> findByCustomerWithLines(String customerId);

    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}
