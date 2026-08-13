package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.CartItem;
import com.chickenexpress.foodorder.entity.Product;
import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.repository.CartItemRepository;
import com.chickenexpress.foodorder.repository.ProductRepository;
import com.chickenexpress.foodorder.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages the logged-in user's DB-persisted shopping cart.
 *
 * Cart items survive server restarts because they are stored in MySQL.
 * The cart is cleared automatically after a successful order is placed.
 */
@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    /** All cart items for the given user, in insertion order. */
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserIdOrderByAddedAtAsc(userId);
    }

    /** Cart subtotal (sum of quantity × unit price for all items). */
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(Long userId) {
        return getCartItems(userId).stream()
            .map(item -> item.getProduct().getPrice()
                             .multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Number of distinct line items in the cart (for the nav badge). */
    @Transactional(readOnly = true)
    public long getCartItemCount(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }

    // ── Mutate ───────────────────────────────────────────────────────────────

    /**
     * Add a product to the cart or increment its quantity if already present.
     *
     * @param userId    the logged-in user's ID
     * @param productId the product to add
     * @param quantity  quantity to add (must be ≥ 1)
     */
    public CartItem addToCart(Long userId, Long productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1.");

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (!product.isAvailable()) {
            throw new IllegalStateException("Product is currently unavailable: " + product.getName());
        }

        return cartItemRepository.findByUserIdAndProductId(userId, productId)
            .map(existing -> {
                existing.setQuantity(existing.getQuantity() + quantity);
                return cartItemRepository.save(existing);
            })
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                CartItem item = new CartItem(user, product, quantity);
                return cartItemRepository.save(item);
            });
    }

    /**
     * Set a cart item's quantity to a specific value.
     * Removes the item if quantity ≤ 0.
     */
    public void updateQuantity(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            removeFromCart(userId, productId);
            return;
        }
        cartItemRepository.findByUserIdAndProductId(userId, productId).ifPresent(item -> {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        });
    }

    /** Remove a single product from the cart. */
    public void removeFromCart(Long userId, Long productId) {
        cartItemRepository.findByUserIdAndProductId(userId, productId)
            .ifPresent(cartItemRepository::delete);
    }

    /** Clear the entire cart (called after order placement). */
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
