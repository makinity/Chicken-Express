package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.repository.OrderItemRepository;
import com.chickenexpress.foodorder.repository.OrderRepository;
import com.chickenexpress.foodorder.repository.ProductRepository;
import com.chickenexpress.foodorder.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin dashboard — stats, revenue, and Chart.js data.
 */
@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository   productRepository;
    private final UserRepository      userRepository;

    public DashboardController(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               ProductRepository productRepository,
                               UserRepository userRepository) {
        this.orderRepository     = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository   = productRepository;
        this.userRepository      = userRepository;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime sevenDaysAgo = now.minusDays(6).toLocalDate().atStartOfDay();

        // ── Stat cards ────────────────────────────────────────────────────
        model.addAttribute("totalOrders",   orderRepository.count());
        model.addAttribute("todayOrders",   orderRepository.countByCreatedAtBetween(todayStart, now));
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalUsers",    userRepository.count());
        model.addAttribute("totalRevenue",  orderRepository.sumTotalRevenue());
        model.addAttribute("todayRevenue",  orderRepository.sumRevenueBetween(todayStart, now));
        model.addAttribute("monthRevenue",  orderRepository.sumRevenueBetween(monthStart, now));

        // ── Live order queues ─────────────────────────────────────────────
        model.addAttribute("pendingOrders",
            orderRepository.findByStatusOrderByCreatedAtAsc(Order.Status.PENDING));
        model.addAttribute("preparingOrders",
            orderRepository.findByStatusOrderByCreatedAtAsc(Order.Status.PREPARING));

        // ── Chart: 7-day daily revenue ────────────────────────────────────
        // Build a full 7-slot map so days with no sales still appear as 0
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE d");
        Map<LocalDate, BigDecimal> revenueByDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            revenueByDay.put(LocalDate.now().minusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : orderRepository.dailyRevenueSince(sevenDaysAgo)) {
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            BigDecimal rev = new BigDecimal(row[1].toString());
            if (revenueByDay.containsKey(date)) revenueByDay.put(date, rev);
        }
        List<String>     salesLabels = new ArrayList<>();
        List<BigDecimal> salesValues = new ArrayList<>();
        revenueByDay.forEach((d, v) -> {
            salesLabels.add(d.format(dayFmt));
            salesValues.add(v);
        });
        model.addAttribute("salesLabels", salesLabels);
        model.addAttribute("salesValues", salesValues);

        // ── Chart: orders by status ───────────────────────────────────────
        // Build a full map for all statuses so empty ones show as 0
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Order.Status s : Order.Status.values()) statusMap.put(s.name(), 0L);
        for (Object[] row : orderRepository.countByStatus()) {
            statusMap.put(((Order.Status) row[0]).name(), (Long) row[1]);
        }
        model.addAttribute("statusLabels", new ArrayList<>(statusMap.keySet()));
        model.addAttribute("statusValues", new ArrayList<>(statusMap.values()));

        // ── Chart: top 5 products ─────────────────────────────────────────
        List<String> topLabels = new ArrayList<>();
        List<Long>   topValues = new ArrayList<>();
        for (Object[] row : orderItemRepository.findTopProductsByQuantity(PageRequest.of(0, 5))) {
            topLabels.add((String) row[0]);
            topValues.add((Long) row[1]);
        }
        model.addAttribute("topProductLabels", topLabels);
        model.addAttribute("topProductValues", topValues);

        return "admin/dashboard";
    }
}
