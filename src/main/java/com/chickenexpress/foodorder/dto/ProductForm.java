package com.chickenexpress.foodorder.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Form-backing DTO for the admin product create/edit form.
 * Validated by Spring's @Valid before being mapped to a Product entity.
 */
public class ProductForm {

    @NotBlank(message = "Product name is required.")
    @Size(max = 150, message = "Name must not exceed 150 characters.")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters.")
    private String description;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "1.00", message = "Price must be at least ₱1.00.")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid.")
    private BigDecimal price;

    @NotNull(message = "Please select a category.")
    private Long categoryId;

    private boolean available = true;
    private boolean popular = false;
    private boolean spicy = false;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public boolean isPopular() { return popular; }
    public void setPopular(boolean popular) { this.popular = popular; }

    public boolean isSpicy() { return spicy; }
    public void setSpicy(boolean spicy) { this.spicy = spicy; }
}
