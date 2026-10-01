package com.orderdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderdesk.domain.PaymentAttempt;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    long countByOrderIdAndOutcome(Long orderId, String outcome);
}
