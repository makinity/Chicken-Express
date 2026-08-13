package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CartItem entities.
 * Used by CartService to read, update, and clear a user's cart.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** All cart items for a given user. */
    List<CartItem> findByUserIdOrderByAddedAtAsc(Long userId);

    /** A specific product in a user's cart (unique constraint on user + product). */
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    /** Remove all items from a user's cart (called after order placement). */
    void deleteByUserId(Long userId);

    /** Count of distinct items in the user's cart (for the cart badge). */
    long countByUserId(Long userId);
}
