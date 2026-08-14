package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.dto.ReceiptRow;
import com.chickenexpress.foodorder.dto.SalesReportRow;
import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.entity.OrderItem;
import com.chickenexpress.foodorder.repository.OrderRepository;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates admin reports.
 *
 * <p>Supported export formats:</p>
 * <ul>
 *   <li>Excel (.xlsx) via Apache POI — sales report by date range</li>
 *   <li>PDF via JasperReports — sales report and per-order receipt</li>
 * </ul>
 *
 * <p>JasperReports templates are loaded from the classpath at
 * {@code /reports/sales_report.jrxml} and {@code /reports/receipt.jrxml}.</p>
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final String PESO = "\u20B1";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ── Excel Export ─────────────────────────────────────────────────────────

    /**
     * Generate a sales report as an Excel workbook for the given date range.
     * Only includes orders with status COMPLETED.
     *
     * @param from start of date range (inclusive)
     * @param to   end of date range (inclusive)
     * @return byte array of the .xlsx file
     */
    public byte[] generateSalesReportXlsx(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByDateRange(from, to)
                .stream()
                .filter(o -> o.getStatus() == Order.Status.COMPLETED)
                .collect(Collectors.toList());

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
            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getCreatedAt().format(DATE_FMT));
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

    // ── PDF: Sales Report ────────────────────────────────────────────────────

    /**
     * Generate a PDF sales report using {@code sales_report.jrxml}.
     * Only includes COMPLETED orders in the given date range.
     *
     * @param from start of date range (inclusive)
     * @param to   end of date range (inclusive)
     * @return byte array of the .pdf file
     */
    public byte[] generateSalesReportPdf(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByDateRange(from, to)
                .stream()
                .filter(o -> o.getStatus() == Order.Status.COMPLETED)
                .collect(Collectors.toList());

        // Build JasperReports datasource rows
        List<SalesReportRow> rows = orders.stream()
                .map(o -> new SalesReportRow(
                        o.getOrderNumber(),
                        o.getCreatedAt().format(DATE_FMT),
                        o.getUser().getFullName(),
                        o.getOrderItems().size(),
                        formatPeso(o.getTotalAmount()),
                        o.getStatus().name(),
                        o.getPayment() != null ? o.getPayment().getStatus().name() : "N/A"
                ))
                .collect(Collectors.toList());

        // Grand total
        BigDecimal totalSales = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // JasperReports parameters
        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_FROM", from.toLocalDate().format(SHORT_FMT));
        params.put("REPORT_TO",   to.toLocalDate().format(SHORT_FMT));
        params.put("TOTAL_SALES", formatPeso(totalSales));

        return fillAndExportPdf("/reports/sales_report.jrxml", params, rows);
    }

    // ── PDF: Receipt ─────────────────────────────────────────────────────────

    /**
     * Generate a PDF receipt for a single order using {@code receipt.jrxml}.
     *
     * @param orderId the order's database ID
     * @return byte array of the .pdf file
     * @throws IllegalArgumentException if the order is not found
     */
    public byte[] generateReceiptPdf(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // Eager-load items (transaction is open — safe to access lazy collection)
        List<OrderItem> items = order.getOrderItems();

        List<ReceiptRow> rows = items.stream()
                .map(item -> new ReceiptRow(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        formatPeso(item.getUnitPrice()),
                        formatPeso(item.getSubtotal())
                ))
                .collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("ORDER_NUMBER",  order.getOrderNumber());
        params.put("ORDER_DATE",    order.getCreatedAt().format(DATE_FMT));
        params.put("CUSTOMER_NAME", order.getUser().getFullName());
        params.put("ORDER_TYPE",    order.getOrderType().name());
        params.put("TOTAL_AMOUNT",  formatPeso(order.getTotalAmount()));

        return fillAndExportPdf("/reports/receipt.jrxml", params, rows);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Compiles a .jrxml template from the classpath, fills it with the
     * given parameters and bean collection datasource, and exports to PDF bytes.
     */
    private byte[] fillAndExportPdf(String jrxmlClasspath,
                                     Map<String, Object> params,
                                     List<?> beanCollection) {
        try (InputStream jrxmlStream = getClass().getResourceAsStream(jrxmlClasspath)) {
            if (jrxmlStream == null) {
                throw new RuntimeException("JasperReports template not found on classpath: " + jrxmlClasspath);
            }

            JasperReport compiled = JasperCompileManager.compileReport(jrxmlStream);

            JRBeanCollectionDataSource dataSource =
                    new JRBeanCollectionDataSource(beanCollection);

            JasperPrint print = JasperFillManager.fillReport(compiled, params, dataSource);

            return JasperExportManager.exportReportToPdf(print);

        } catch (JRException e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JasperReports template: " + e.getMessage(), e);
        }
    }

    /** Formats a BigDecimal as a peso string, e.g. "₱1,234.50". */
    private String formatPeso(BigDecimal amount) {
        if (amount == null) return PESO + "0.00";
        return PESO + String.format("%,.2f", amount);
    }
}
