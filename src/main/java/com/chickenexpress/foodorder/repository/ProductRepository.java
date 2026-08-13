package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Product entities.
 * Used by the menu display, admin product management, and cart service.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** All available products in a given category, ordered by name. */
    List<Product> findByCategoryIdAndAvailableTrueOrderByNameAsc(Long categoryId);

    /** All available products, ordered by category sort order then product name. */
    @Query("SELECT p FROM Product p JOIN p.category c WHERE p.available = true ORDER BY c.sortOrder ASC, p.name ASC")
    List<Product> findAllAvailableOrderedByCategoryAndName();

    /** Search products by name or description (case-insensitive). */
    @Query("SELECT p FROM Product p WHERE p.available = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    /** Products marked as popular — shown in featured/highlights section. */
    List<Product> findByPopularTrueAndAvailableTrue();
}
