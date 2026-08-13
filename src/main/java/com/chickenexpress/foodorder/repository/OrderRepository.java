package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entities.
 * Used by customer order history, admin order management, and report generation.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** All orders placed by a specific customer, newest first. */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** All orders, newest first — for admin order list. */
    List<Order> findAllByOrderByCreatedAtDesc();

    /** Find a single order by its human-readable order number. */
    Optional<Order> findByOrderNumber(String orderNumber);

    /** All orders in a given status (for admin order board). */
    List<Order> findByStatusOrderByCreatedAtAsc(Order.Status status);

    /** All orders in the given date range — for sales reports. */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Count of orders placed between two timestamps — used for dashboard stats. */
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /** Total revenue (sum of totalAmount) for COMPLETED orders — all time. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    java.math.BigDecimal sumTotalRevenue();

    /** Total revenue for COMPLETED orders within a date range. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :from AND :to")
    java.math.BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Daily revenue for the last N days — returns [date_string, revenue] pairs. */
    @Query(value = "SELECT DATE(o.created_at) as day, COALESCE(SUM(o.total_amount), 0) as rev " +
                   "FROM orders o WHERE o.status = 'COMPLETED' AND o.created_at >= :from " +
                   "GROUP BY DATE(o.created_at) ORDER BY day ASC",
           nativeQuery = true)
    List<Object[]> dailyRevenueSince(@Param("from") LocalDateTime from);

    /** Count of orders grouped by status. */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();

    /** Update createdAt directly — used by DataInitializer to backdate seed orders. */
    @Modifying
    @jakarta.transaction.Transactional
    @Query("UPDATE Order o SET o.createdAt = :createdAt, o.updatedAt = :createdAt WHERE o.id = :id")
    void updateCreatedAt(@Param("id") Long id, @Param("createdAt") LocalDateTime createdAt);
}
