package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.Category;
import com.chickenexpress.foodorder.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ProductRepository using @DataJpaTest.
 *
 * Runs against an in-memory H2 database (auto-configured by Spring Boot test).
 * No full context — JPA slice only.
 *
 * To run, add H2 as a test dependency in pom.xml:
 *   <dependency>
 *     <groupId>com.h2database</groupId>
 *     <artifactId>h2</artifactId>
 *     <scope>test</scope>
 *   </dependency>
 */
@DataJpaTest
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {Controller.class, ControllerAdvice.class})
})
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Category chickenCategory;

    @BeforeEach
    void setUp() {
        chickenCategory = entityManager.persistAndFlush(new Category("Chicken Meals"));
    }

    // ── findAllAvailableOrdered ───────────────────────────────────────────────

    @Test
    @DisplayName("findAllAvailableOrdered: returns only available products")
    void findAllAvailable_excludesUnavailableProducts() {
        Product available = new Product("Fried Chicken Solo", new BigDecimal("99.00"), chickenCategory);
        available.setAvailable(true);
        entityManager.persistAndFlush(available);

        Product unavailable = new Product("Sold Out Meal", new BigDecimal("150.00"), chickenCategory);
        unavailable.setAvailable(false);
        entityManager.persistAndFlush(unavailable);

        List<Product> products = productRepository.findAllAvailableOrderedByCategoryAndName();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Fried Chicken Solo");
    }

    // ── searchByKeyword ──────────────────────────────────────────────────────

    @Test
    @DisplayName("searchByKeyword: finds product by partial name (case-insensitive)")
    void searchByKeyword_findsByPartialName() {
        Product product = new Product("Spicy Chicken Thighs", new BigDecimal("120.00"), chickenCategory);
        product.setAvailable(true);
        entityManager.persistAndFlush(product);

        List<Product> results = productRepository.searchByKeyword("spicy");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Spicy Chicken Thighs");
    }

    @Test
    @DisplayName("searchByKeyword: returns empty list when no keyword match")
    void searchByKeyword_returnsEmpty_whenNoMatch() {
        List<Product> results = productRepository.searchByKeyword("nonexistent");
        assertThat(results).isEmpty();
    }

    // ── findByPopularTrue ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPopularTrue: returns only popular available products")
    void findByPopularTrue_returnsPopularAvailableProducts() {
        Product popular = new Product("Chicken Wings", new BigDecimal("180.00"), chickenCategory);
        popular.setAvailable(true);
        popular.setPopular(true);
        entityManager.persistAndFlush(popular);

        Product notPopular = new Product("Chicken Strips", new BigDecimal("130.00"), chickenCategory);
        notPopular.setAvailable(true);
        notPopular.setPopular(false);
        entityManager.persistAndFlush(notPopular);

        List<Product> results = productRepository.findByPopularTrueAndAvailableTrue();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Chicken Wings");
    }
}
