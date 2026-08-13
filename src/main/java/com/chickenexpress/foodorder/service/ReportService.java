package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.repository.OrderRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates admin reports.
 *
 * Supported export formats:
 * - Excel (.xlsx) via Apache POI — sales report by date range
 * - PDF via JasperReports — TODO: implement in Phase 2 after .jrxml templates are designed
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ── Excel Export ─────────────────────────────────────────────────────────

    /**
     * Generate a sales report as an Excel workbook for the given date range.
     *
     * @param from start of date range (inclusive)
     * @param to   end of date range (inclusive)
     * @return byte array of the .xlsx file contents
     */
    public byte[] generateSalesReportXlsx(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByDateRange(from, to);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sales Report");

            // ── Header row ──────────────────────────────────────────────────
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Order #", "Date", "Customer", "Items", "Total (PHP)", "Status", "Payment"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ───────────────────────────────────────────────────
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int rowIdx = 1;

            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getCreatedAt().format(formatter));
                row.createCell(2).setCellValue(order.getUser().getFullName());
                row.createCell(3).setCellValue(order.getOrderItems().size());
                row.createCell(4).setCellValue(order.getTotalAmount().doubleValue());
                row.createCell(5).setCellValue(order.getStatus().name());
                row.createCell(6).setCellValue(
                    order.getPayment() != null ? order.getPayment().getStatus().name() : "N/A");
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report.", e);
        }
    }

    // ── PDF Export (JasperReports) ───────────────────────────────────────────

    /**
     * Generate a PDF receipt for a single order.
     * TODO: implement after receipt.jrxml is finalized.
     */
    public byte[] generateReceiptPdf(Long orderId) {
        throw new UnsupportedOperationException("PDF receipt generation not yet implemented.");
    }

    /**
     * Generate a PDF sales report for the given date range.
     * TODO: implement after sales_report.jrxml is finalized.
     */
    public byte[] generateSalesReportPdf(LocalDateTime from, LocalDateTime to) {
        throw new UnsupportedOperationException("PDF sales report generation not yet implemented.");
    }
}
