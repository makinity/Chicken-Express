package com.chickenexpress.foodorder.dto;

import com.chickenexpress.foodorder.entity.Order;
import jakarta.validation.constraints.Size;

/**
 * Form-backing DTO for the customer checkout page.
 * Captures order type, optional notes, and delivery address.
 */
public class CheckoutRequest {

    private Order.OrderType orderType = Order.OrderType.TAKEOUT;

    @Size(max = 500, message = "Notes must not exceed 500 characters.")
    private String notes;

    @Size(max = 255, message = "Delivery address must not exceed 255 characters.")
    private String deliveryAddress;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Order.OrderType getOrderType() { return orderType; }
    public void setOrderType(Order.OrderType orderType) { this.orderType = orderType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
