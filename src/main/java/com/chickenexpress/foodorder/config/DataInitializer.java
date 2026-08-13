package com.chickenexpress.foodorder.config;

import com.chickenexpress.foodorder.entity.*;
import com.chickenexpress.foodorder.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds the database with sample data on first startup.
 * Idempotent — skips if products already exist.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository  categoryRepository;
    private final ProductRepository   productRepository;
    private final UserRepository      userRepository;
    private final OrderRepository     orderRepository;
    private final PasswordEncoder     passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository  = productRepository;
        this.userRepository     = userRepository;
        this.orderRepository    = orderRepository;
        this.passwordEncoder    = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            log.info("Database already seeded — skipping.");
            return;
        }

        log.info("Seeding database...");

        // ── Categories ───────────────────────────────────────────────────
        Category chickenMeals = cat("Chicken Meals",    "Fried, grilled, and spicy chicken",           1);
        Category combos       = cat("Combos & Bundles", "Meal combos for sharing or solo feasting",    2);
        Category sides        = cat("Sides",            "Perfect pairings for your chicken",           3);
        Category drinks       = cat("Drinks",           "Refreshing beverages",                        4);
        Category desserts     = cat("Desserts",         "Sweet treats to end your meal",               5);

        // ── Products ─────────────────────────────────────────────────────
        Product cfcChicken  = prod("Classic Fried Chicken (2 pcs)", "Crispy golden-brown chicken marinated in 12 herbs and spices.", new BigDecimal("89.00"),  chickenMeals, true,  false);
        Product spicyChicken= prod("Spicy Fried Chicken (2 pcs)",   "Same crispy goodness with a fiery chili kick.",                 new BigDecimal("95.00"),  chickenMeals, true,  true);
        Product inasal      = prod("Chicken Inasal (1 pc)",          "Grilled chicken in lemongrass, calamansi, and annatto oil.",   new BigDecimal("110.00"), chickenMeals, true,  false);
        Product bbq         = prod("Chicken BBQ (1 pc)",             "Smoky charcoal-grilled chicken with sweet BBQ glaze.",         new BigDecimal("120.00"), chickenMeals, false, false);
        Product popcorn     = prod("Chicken Popcorn (Regular)",      "Bite-sized chicken pieces, perfectly seasoned and fried.",     new BigDecimal("75.00"),  chickenMeals, false, false);
        Product wings       = prod("Hot Chicken Wings (6 pcs)",      "Bone-in wings tossed in signature hot sauce.",                 new BigDecimal("149.00"), chickenMeals, true,  true);

        Product soloA       = prod("Solo Combo A",         "2 pcs chicken + rice + drink.",                            new BigDecimal("129.00"), combos, true,  false);
        Product soloB       = prod("Solo Combo B",         "3 pcs chicken + rice + drink + side.",                     new BigDecimal("179.00"), combos, true,  false);
        Product familyBucket= prod("Family Bucket (6 pcs)","6 pcs chicken + 3 rice + 3 drinks + 2 sides.",             new BigDecimal("499.00"), combos, true,  false);
        Product partyPlatter= prod("Party Platter (12 pcs)","12 pcs + 6 rice + 6 drinks + 4 sides. Perfect for parties!", new BigDecimal("899.00"), combos, false, false);
        Product barkada     = prod("Barkada Deal (8 pcs)", "8 pcs + 4 rice + 4 drinks + 3 sides.",                     new BigDecimal("649.00"), combos, true,  false);

        Product garlicRice  = prod("Garlic Rice",           "Buttery garlic fried rice.",                 new BigDecimal("35.00"), sides, false, false);
        Product mashedPotato= prod("Mashed Potato",         "Creamy mashed potato with gravy.",           new BigDecimal("45.00"), sides, false, false);
        Product coleslaw    = prod("Coleslaw",              "Crunchy slaw in tangy dressing.",            new BigDecimal("35.00"), sides, false, false);
        prod("Corn on the Cob",      "Sweet buttered grilled corn.",               new BigDecimal("45.00"), sides, false, false);
        prod("French Fries (Regular)","Golden crispy seasoned fries.",             new BigDecimal("55.00"), sides, false, false);
        prod("Mac & Cheese",         "Creamy cheddar macaroni.",                   new BigDecimal("65.00"), sides, false, false);

        Product icedTea     = prod("Iced Tea (Regular)", "Refreshing iced tea with lemon.",        new BigDecimal("35.00"), drinks, false, false);
        prod("Iced Tea (Large)",     "Larger iced tea for bigger thirsts.",        new BigDecimal("49.00"), drinks, false, false);
        Product softDrink   = prod("Soft Drink (Regular)","Coke, Sprite, or Royal.",               new BigDecimal("30.00"), drinks, false, false);
        prod("Soft Drink (Large)",   "Your favorite soda in a bigger cup.",        new BigDecimal("45.00"), drinks, false, false);
        prod("Bottled Water",        "Pure drinking water, 500ml.",                new BigDecimal("20.00"), drinks, false, false);
        prod("Fresh Lemonade",       "Freshly squeezed lemonade with honey.",      new BigDecimal("55.00"), drinks, false, false);

        prod("Chocolate Brownie",    "Rich fudgy brownie served warm.",            new BigDecimal("55.00"), desserts, false, false);
        prod("Halo-Halo (Regular)",  "Classic Filipino shaved ice dessert.",       new BigDecimal("75.00"), desserts, true,  false);
        prod("Ice Cream (Single Scoop)", "Chocolate, vanilla, or strawberry.",     new BigDecimal("35.00"), desserts, false, false);

        // ── Users — keep admin, add 6 customers ──────────────────────────
        User admin   = user("Admin",            "admin@chickenexpress.com", "admin123",    "ROLE_ADMIN");
        User maria   = user("Maria Santos",     "maria@example.com",        "customer123", "ROLE_CUSTOMER");
        User jose    = user("Jose Reyes",       "jose@example.com",         "customer123", "ROLE_CUSTOMER");
        User ana     = user("Ana Garcia",       "ana@example.com",          "customer123", "ROLE_CUSTOMER");
        User carlo   = user("Carlo Mendoza",    "carlo@example.com",        "customer123", "ROLE_CUSTOMER");
        User lea     = user("Lea Villanueva",   "lea@example.com",          "customer123", "ROLE_CUSTOMER");
        User marco   = user("Marco Bautista",   "marco@example.com",        "customer123", "ROLE_CUSTOMER");

        // ── Orders — spread over last 7 days with realistic data ──────────
        LocalDateTime now = LocalDateTime.now();

        // Day 7 ago
        order("CE-20260806-0001", maria, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(6).withHour(10).withMinute(15),
              items(cfcChicken, 2, soloA, 1));
        order("CE-20260806-0002", jose,  Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(6).withHour(12).withMinute(30),
              items(familyBucket, 1, icedTea, 3));
        order("CE-20260806-0003", ana,   Order.Status.CANCELLED, Order.OrderType.TAKEOUT,  now.minusDays(6).withHour(14).withMinute(5),
              items(spicyChicken, 1, garlicRice, 1));

        // Day 6 ago
        order("CE-20260807-0001", carlo, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(5).withHour(9).withMinute(45),
              items(soloB, 2, softDrink, 2));
        order("CE-20260807-0002", lea,   Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(5).withHour(13).withMinute(20),
              items(barkada, 1, icedTea, 4));
        order("CE-20260807-0003", marco, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(5).withHour(18).withMinute(0),
              items(wings, 2, mashedPotato, 2, softDrink, 2));

        // Day 5 ago
        order("CE-20260808-0001", maria, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(4).withHour(11).withMinute(10),
              items(cfcChicken, 3, coleslaw, 2, icedTea, 2));
        order("CE-20260808-0002", jose,  Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(4).withHour(14).withMinute(55),
              items(inasal, 2, garlicRice, 2));
        order("CE-20260808-0003", ana,   Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(4).withHour(17).withMinute(30),
              items(partyPlatter, 1));

        // Day 4 ago
        order("CE-20260809-0001", carlo, Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(3).withHour(10).withMinute(0),
              items(soloA, 1, soloB, 1));
        order("CE-20260809-0002", lea,   Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(3).withHour(12).withMinute(15),
              items(popcorn, 2, softDrink, 2));
        order("CE-20260809-0003", marco, Order.Status.CANCELLED, Order.OrderType.TAKEOUT,  now.minusDays(3).withHour(16).withMinute(40),
              items(bbq, 2, garlicRice, 2));
        order("CE-20260809-0004", maria, Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(3).withHour(19).withMinute(5),
              items(familyBucket, 1, icedTea, 2));

        // Day 3 ago
        order("CE-20260810-0001", jose,  Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(2).withHour(9).withMinute(30),
              items(cfcChicken, 2, mashedPotato, 1, icedTea, 1));
        order("CE-20260810-0002", ana,   Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(2).withHour(11).withMinute(45),
              items(spicyChicken, 2, coleslaw, 1, softDrink, 2));
        order("CE-20260810-0003", carlo, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(2).withHour(15).withMinute(20),
              items(soloA, 3));
        order("CE-20260810-0004", lea,   Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(2).withHour(18).withMinute(50),
              items(wings, 1, garlicRice, 1, icedTea, 1));

        // Day 2 ago (yesterday)
        order("CE-20260811-0001", marco, Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(1).withHour(10).withMinute(5),
              items(barkada, 1, softDrink, 3));
        order("CE-20260811-0002", maria, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(1).withHour(12).withMinute(30),
              items(inasal, 1, garlicRice, 1, icedTea, 1));
        order("CE-20260811-0003", jose,  Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.minusDays(1).withHour(14).withMinute(0),
              items(cfcChicken, 4, mashedPotato, 2, softDrink, 2));
        order("CE-20260811-0004", ana,   Order.Status.CANCELLED, Order.OrderType.TAKEOUT,  now.minusDays(1).withHour(16).withMinute(15),
              items(soloB, 1));
        order("CE-20260811-0005", carlo, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.minusDays(1).withHour(19).withMinute(20),
              items(familyBucket, 1, icedTea, 2));

        // Today
        order("CE-20260812-0001", lea,   Order.Status.COMPLETED, Order.OrderType.DINE_IN,  now.withHour(9).withMinute(10),
              items(soloA, 2, icedTea, 2));
        order("CE-20260812-0002", marco, Order.Status.COMPLETED, Order.OrderType.TAKEOUT,  now.withHour(10).withMinute(45),
              items(cfcChicken, 2, coleslaw, 1));
        order("CE-20260812-0003", maria, Order.Status.PREPARING, Order.OrderType.DINE_IN,  now.withHour(11).withMinute(30),
              items(spicyChicken, 2, garlicRice, 2, softDrink, 2));
        order("CE-20260812-0004", jose,  Order.Status.PENDING,   Order.OrderType.TAKEOUT,  now.withHour(11).withMinute(55),
              items(soloB, 1, icedTea, 1));
        order("CE-20260812-0005", ana,   Order.Status.PENDING,   Order.OrderType.TAKEOUT,  now.withHour(12).withMinute(10),
              items(wings, 1, mashedPotato, 1, softDrink, 1));

        log.info("Seeding complete: 5 categories, 26 products, 7 users, 28 orders.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Category cat(String name, String desc, int sort) {
        Category c = new Category(name);
        c.setDescription(desc);
        c.setSortOrder(sort);
        c.setActive(true);
        return categoryRepository.save(c);
    }

    private Product prod(String name, String desc, BigDecimal price,
                         Category cat, boolean popular, boolean spicy) {
        Product p = new Product(name, price, cat);
        p.setDescription(desc);
        p.setAvailable(true);
        p.setPopular(popular);
        p.setSpicy(spicy);
        return productRepository.save(p);
    }

    private User user(String name, String email, String pass, String role) {
        User u = new User(name, email, passwordEncoder.encode(pass));
        u.setRole(role);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    /** Build a flat array of (product, quantity) pairs. */
    private Object[] items(Object... args) { return args; }

    private void order(String number, User customer, Order.Status status,
                       Order.OrderType type, LocalDateTime at, Object[] itemArgs) {
        Order o = new Order();
        o.setOrderNumber(number);
        o.setStatus(status);
        o.setOrderType(type);
        o.setUser(customer);

        // parse (product, qty) pairs
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < itemArgs.length; i += 2) {
            Product p   = (Product) itemArgs[i];
            int     qty = (int) itemArgs[i + 1];
            OrderItem item = new OrderItem(o, p, qty, p.getPrice());
            o.getOrderItems().add(item);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        o.setTotalAmount(total);

        // manually set createdAt for historical data
        o = orderRepository.save(o);

        // update timestamps via JPQL to bypass @PrePersist
        orderRepository.updateCreatedAt(o.getId(), at);
    }
}
