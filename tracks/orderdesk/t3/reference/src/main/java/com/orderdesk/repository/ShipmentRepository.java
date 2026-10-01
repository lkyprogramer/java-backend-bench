package com.orderdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.orderdesk.domain.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @Query("select distinct s from Shipment s left join fetch s.lines where s.orderId = :orderId order by s.id")
    List<Shipment> findByOrderIdWithLines(Long orderId);
}
