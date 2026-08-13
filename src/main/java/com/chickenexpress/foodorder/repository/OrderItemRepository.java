package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Top N products by total units sold across all orders. */
    @Query("SELECT oi.product.name, SUM(oi.quantity) as total " +
           "FROM OrderItem oi GROUP BY oi.product.name ORDER BY total DESC")
    List<Object[]> findTopProductsByQuantity(org.springframework.data.domain.Pageable pageable);
}
