package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.repository.OrderRepository;
import com.chickenexpress.foodorder.service.ReportService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin reports page.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET  /admin/reports                  — main page with summary stats + order preview</li>
 *   <li>GET  /admin/reports/sales/xlsx        — download Excel sales report</li>
 *   <li>GET  /admin/reports/sales/pdf         — download PDF sales report (JasperReports)</li>
 *   <li>GET  /admin/reports/receipt/{orderId} — download PDF receipt for a single order</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/reports")
public class ReportsController {

    private final ReportService reportService;
    private final OrderRepository orderRepository;

    public ReportsController(ReportService reportService, OrderRepository orderRepository) {
        this.reportService   = reportService;
        this.orderRepository = orderRepository;
    }

    // ── Reports page ─────────────────────────────────────────────────────────

    /**
     * Main reports page. Accepts optional {@code from}/{@code to} parameters for
     * the preview table and summary stats. Defaults to the current month if omitted.
     */
    @GetMapping
    public String reportsPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        // Default: first day of current month → today
        LocalDate today      = LocalDate.now();
        LocalDate effectiveFrom = (from != null) ? from : today.withDayOfMonth(1);
        LocalDate effectiveTo   = (to   != null) ? to   : today;

        LocalDateTime fromDt = effectiveFrom.atStartOfDay();
        LocalDateTime toDt   = effectiveTo.plusDays(1).atStartOfDay().minusSeconds(1);

        // Orders in range (all statuses) for the preview table
        List<Order> ordersInRange = orderRepository.findByDateRange(fromDt, toDt);

        // Completed orders for revenue stats
        List<Order> completedOrders = ordersInRange.stream()
                .filter(o -> o.getStatus() == Order.Status.COMPLETED)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Status breakdown counts
        long pendingCount   = ordersInRange.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count();
        long preparingCount = ordersInRange.stream().filter(o -> o.getStatus() == Order.Status.PREPARING).count();
        long completedCount = completedOrders.size();
        long cancelledCount = ordersInRange.stream().filter(o -> o.getStatus() == Order.Status.CANCELLED).count();

        model.addAttribute("today",          today);
        model.addAttribute("from",           effectiveFrom);
        model.addAttribute("to",             effectiveTo);
        model.addAttribute("orders",         ordersInRange);
        model.addAttribute("totalRevenue",   totalRevenue);
        model.addAttribute("orderCount",     ordersInRange.size());
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount",   pendingCount);
        model.addAttribute("preparingCount", preparingCount);
        model.addAttribute("cancelledCount", cancelledCount);

        return "admin/reports";
    }

    // ── Excel download ────────────────────────────────────────────────────────

    @GetMapping("/sales/xlsx")
    public ResponseEntity<byte[]> downloadSalesXlsx(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay().minusSeconds(1);

        byte[] xlsx = reportService.generateSalesReportXlsx(fromDt, toDt);

        String filename = "sales_report_" + from + "_to_" + to + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    // ── PDF sales report download ─────────────────────────────────────────────

    @GetMapping("/sales/pdf")
    public ResponseEntity<byte[]> downloadSalesPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay().minusSeconds(1);

        byte[] pdf = reportService.generateSalesReportPdf(fromDt, toDt);

        String filename = "sales_report_" + from + "_to_" + to + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── PDF receipt download ──────────────────────────────────────────────────

    @GetMapping("/receipt/{orderId}")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long orderId) {
        byte[] pdf = reportService.generateReceiptPdf(orderId);

        String filename = "receipt_order_" + orderId + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
