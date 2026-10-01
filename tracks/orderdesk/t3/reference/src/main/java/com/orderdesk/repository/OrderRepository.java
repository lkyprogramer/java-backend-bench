package com.orderdesk.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.orderdesk.domain.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select distinct o from Order o left join fetch o.lines where o.customerId = :customerId order by o.id")
    List<Order> findByCustomerWithLines(String customerId);

    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    @EntityGraph(attributePaths = "lines")
    Optional<Order> findWithLinesById(Long id);

    /** Serializes shipments of one order: the shipped-so-far check and the new shipment commit together. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(Long id);
}
