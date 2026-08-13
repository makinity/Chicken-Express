package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Category entities.
 * Used by the menu and admin product management pages.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** All active categories in display order. */
    List<Category> findByActiveTrueOrderBySortOrderAsc();
}
