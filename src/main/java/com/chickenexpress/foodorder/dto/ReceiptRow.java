package com.chickenexpress.foodorder.dto;

/**
 * DTO representing a single line item in a receipt PDF.
 *
 * Field names intentionally match those declared in receipt.jrxml:
 *   productName, quantity, unitPrice, subtotal
 *
 * Used as the datasource for JRBeanCollectionDataSource in ReportService.
 */
public class ReceiptRow {

    private String productName;
    private Integer quantity;
    private String unitPrice;
    private String subtotal;

    public ReceiptRow() {}

    public ReceiptRow(String productName, Integer quantity, String unitPrice, String subtotal) {
        this.productName = productName;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.subtotal    = subtotal;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity()   { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getUnitPrice()   { return unitPrice; }
    public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }

    public String getSubtotal()    { return subtotal; }
    public void setSubtotal(String subtotal) { this.subtotal = subtotal; }
}
