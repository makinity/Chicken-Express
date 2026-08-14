package com.chickenexpress.foodorder.dto;

/**
 * DTO representing a single row in the Sales Report PDF.
 *
 * Field names intentionally match those declared in sales_report.jrxml:
 *   orderNumber, orderDate, customerName, itemCount, totalAmount, status, paymentStatus
 *
 * Used as the datasource for JRBeanCollectionDataSource in ReportService.
 */
public class SalesReportRow {

    private String orderNumber;
    private String orderDate;
    private String customerName;
    private Integer itemCount;
    private String totalAmount;
    private String status;
    private String paymentStatus;

    public SalesReportRow() {}

    public SalesReportRow(String orderNumber, String orderDate, String customerName,
                          Integer itemCount, String totalAmount,
                          String status, String paymentStatus) {
        this.orderNumber   = orderNumber;
        this.orderDate     = orderDate;
        this.customerName  = customerName;
        this.itemCount     = itemCount;
        this.totalAmount   = totalAmount;
        this.status        = status;
        this.paymentStatus = paymentStatus;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getOrderNumber()   { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getOrderDate()     { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public String getCustomerName()  { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Integer getItemCount()    { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public String getTotalAmount()   { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus()        { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
