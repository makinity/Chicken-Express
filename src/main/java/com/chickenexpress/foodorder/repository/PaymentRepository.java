package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Payment entities.
 * Used by PaymentService to look up payments by PayMongo session ID
 * when processing incoming webhooks.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Find a payment record by PayMongo checkout session ID. */
    Optional<Payment> findByPaymongoSessionId(String paymongoSessionId);

    /** Find a payment record by the order it belongs to. */
    Optional<Payment> findByOrderId(Long orderId);
}
