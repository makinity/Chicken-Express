# ChickenExpress — Project Plan (Web Application)

## 0. Architecture Change Notice

This project was originally planned as a **JavaFX desktop application**. The client has clarified they want an actual **website**, so the plan has pivoted to a **Spring Boot web application** built and edited in NetBeans (NetBeans supports Maven-based Spring Boot projects as a code editor; it does not offer the deep visual FXML/Scene Builder tooling used in the desktop version).

Key differences from the desktop plan:
- Runs on a server, accessed via browser at a URL — not installed as a standalone app
- UI is rendered as HTML/CSS/JS (server-rendered via Thymeleaf) instead of FXML
- PayMongo integration now uses **real webhooks** instead of client-side polling, since a backend server exists to receive them
- Data access uses **Spring Data JPA** (Hibernate) instead of hand-written JDBC DAOs
- Authentication/authorization uses **Spring Security** instead of custom BCrypt + session logic

---

## 1. Project Overview

**Project name:** ChickenExpress
**Type:** Web-based food ordering system
**Cuisine focus:** Chicken-based dishes (fried chicken, chicken meals/combos, chicken-based sides and specials)
**Platform:** Spring Boot web application (server-rendered with Thymeleaf)
**Database:** MySQL
**IDE:** NetBeans (Maven project)
**Payment Integration:** PayMongo (REST API, hosted checkout session, confirmed via webhook)

ChickenExpress is a web-based ordering system for a chicken-focused food business, with two interfaces:
- **Customer interface** — browse menu, cart, checkout, pay via PayMongo, track orders — accessible from any browser
- **Admin interface** — manage products, orders, users, view dashboard analytics, generate reports — a protected `/admin` section of the same site

---

## 2. System Users & Core Features

### 2.1 Customer
- Register / Login (Spring Security, BCrypt-hashed passwords)
- Browse product catalog (categorized: Chicken Meals, Combos/Bundles, Sides, Drinks, Desserts)
- Search and filter menu items
- Add items to cart, adjust quantities (session-based or DB-persisted cart)
- Checkout (dine-in / takeout / delivery — to be finalized)
- Pay via PayMongo (cards, GCash, GrabPay, Maya) through hosted checkout redirect
- View order status and order history
- Manage profile / request account deactivation

### 2.2 Admin
- Admin login (role-based, same login system with `ROLE_ADMIN`)
- Dashboard with sales/analytics summary (Chart.js or similar, rendered via Thymeleaf + JS)
- Product/inventory management (CRUD)
- Order management (update status: pending → preparing → ready → completed)
- User management (activate/deactivate customer accounts)
- Reports (sales by date range, export to PDF/Excel via JasperReports/Apache POI)
- System settings (promo/banner content, admin profile)

### 2.3 Sample Product Categories (Chicken-Focused Menu)

| Category | Example items |
|---|---|
| Chicken Meals | Fried chicken (solo/2pc/3pc), grilled chicken plate, spicy chicken, chicken wings |
| Combos/Bundles | Chicken + rice + drink combo, family bucket bundle, group meals |
| Sides | Gravy, coleslaw, mashed potato, fries, garlic rice |
| Drinks | Iced tea, soda, bottled water, fruit juice |
| Desserts | Ice cream, halo-halo, brownies |

Actual product data, images, and pricing should reflect this chicken-focused catalog.

---

## 3. Color Palette

Theme: **Green / Yellow (Amber) / Red** — green as primary brand color (fresh/food), amber for highlights and promotions, red reserved for alerts, spicy tags, or destructive actions.

| Role | Hex | Usage |
|---|---|---|
| Primary (Green) | `#639922` | Buttons, active nav, brand accents |
| Primary Light | `#EAF3DE` | Card backgrounds, subtle highlights |
| Primary Dark (text on light green) | `#173404` / `#3B6D11` | Text on green-tinted backgrounds |
| Accent (Amber/Yellow) | `#EF9F27` | Promo badges, "popular" tags, highlights |
| Accent Light | `#FAEEDA` | Promo banners, warm highlight backgrounds |
| Accent Dark (text on light amber) | `#412402` / `#854F0B` | Text on amber-tinted backgrounds |
| Danger (Red) | `#E24B4A` | Spicy tags, remove-item actions, error states |
| Danger Light | `#FCEBEB` | Alert/warning backgrounds |
| Danger Dark (text on light red) | `#501313` / `#A32D2D` | Text on red-tinted backgrounds |

**Usage rules:**
- Green is the primary action color throughout (buttons, active nav, brand mark).
- Amber is used sparingly — promotions, badges — not as a competing primary color.
- Red is reserved for genuinely negative/urgent meaning (errors, spicy indicators, delete actions).

**Implementation:** define these as CSS custom properties in a root stylesheet (`static/css/theme.css`) so the whole site can be retinted from one place:

```css
:root {
    --primary: #639922;
    --primary-light: #EAF3DE;
    --accent-warn: #EF9F27;
    --accent-warn-light: #FAEEDA;
    --danger: #E24B4A;
    --danger-light: #FCEBEB;
}
```

Recommended to pair with a CSS framework for faster, cleaner styling — **Bootstrap 5** or **Tailwind CSS** — customized to use the palette above rather than default framework colors.

---

## 4. Technology Stack

### Core Framework
- **JDK 17 or 21 (LTS)**
- **Spring Boot 3.x** — main application framework
- **Spring Web (MVC)** — REST controllers + server-rendered views
- **Thymeleaf** — server-side HTML templating engine (integrates natively with Spring Boot)
- **Maven** — dependency and build management (NetBeans "Java with Maven" project)

### Database
- **MySQL** — primary relational database
- **Spring Data JPA** (Hibernate) — ORM layer, replaces hand-written DAOs with repository interfaces
- **MySQL Connector/J** — JDBC driver (used under the hood by Spring Data JPA)
- Connection pooling handled automatically by Spring Boot (HikariCP is the default, no manual setup needed)

### Security
- **Spring Security** — authentication, role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`), CSRF protection, password hashing (BCrypt built in)

### Frontend / Styling
- **Thymeleaf** — templating, layout fragments (header/footer/nav reused across pages)
- **Bootstrap 5** or **Tailwind CSS** — responsive layout and components, customized to the green/yellow/red palette
- **Chart.js** — dashboard analytics charts (sales, top products, user activity)
- **Vanilla JS / small jQuery** where needed for cart interactions, form validation, AJAX calls

### Payment Integration
- **PayMongo REST API** — no official Java SDK, integrated via HTTP calls
- **Spring `RestClient`/`RestTemplate`** or **Java 11+ `HttpClient`** — for calling PayMongo endpoints
- **PayMongo Webhooks** — a dedicated `/webhooks/paymongo` REST endpoint receives real-time payment status updates (a major improvement over the desktop app's polling workaround)

### Reports & Exports
- **JasperReports** + **JasperReports Fonts** — receipt/report PDF generation
- **Apache POI** — Excel (.xlsx) export for admin reports

### Data Handling & Utilities
- **Jackson** (included with Spring Web) — JSON serialization/parsing
- **Spring Boot DevTools** — hot reload during development
- **dotenv or `application.properties`/`application.yml` with environment variable placeholders** — keeps DB credentials and PayMongo secret key out of source control

### Testing
- **JUnit 5** + **Spring Boot Test** — unit and integration testing for services/repositories/controllers
- **Mockito** — mocking dependencies in tests

### Deployment
- **Embedded Tomcat** (ships with Spring Boot by default) — no separate server install needed for development
- Packaged as an executable `.jar` (`mvn package`) — deployable to any host supporting Java (e.g., a VPS, Render, Railway, or traditional shared Java hosting)

---

## 5. Folder Structure

```
chickenexpress/
├── pom.xml
├── .gitignore
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/chickenexpress/foodorder/
│   │   │       │
│   │   │       ├── FoodOrderApplication.java     # main Spring Boot entry point
│   │   │       │
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java         # Spring Security setup, roles, CSRF
│   │   │       │   └── WebConfig.java              # static resource handling, interceptors
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── customer/
│   │   │       │   │   ├── AuthController.java       # login/register pages
│   │   │       │   │   ├── MenuController.java
│   │   │       │   │   ├── CartController.java
│   │   │       │   │   ├── CheckoutController.java
│   │   │       │   │   └── OrderHistoryController.java
│   │   │       │   ├── admin/
│   │   │       │   │   ├── DashboardController.java
│   │   │       │   │   ├── ProductManagementController.java
│   │   │       │   │   ├── OrderManagementController.java
│   │   │       │   │   ├── UserManagementController.java
│   │   │       │   │   └── ReportsController.java
│   │   │       │   └── api/
│   │   │       │       └── PayMongoWebhookController.java   # receives PayMongo webhooks
│   │   │       │
│   │   │       ├── entity/                       # JPA entities
│   │   │       │   ├── User.java
│   │   │       │   ├── Product.java
│   │   │       │   ├── Category.java
│   │   │       │   ├── Order.java
│   │   │       │   ├── OrderItem.java
│   │   │       │   ├── CartItem.java
│   │   │       │   └── Payment.java
│   │   │       │
│   │   │       ├── repository/                   # Spring Data JPA repositories
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── ProductRepository.java
│   │   │       │   ├── OrderRepository.java
│   │   │       │   └── PaymentRepository.java
│   │   │       │
│   │   │       ├── service/                       # business logic layer
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── CartService.java
│   │   │       │   ├── OrderService.java
│   │   │       │   ├── PaymentService.java          # PayMongo API orchestration
│   │   │       │   └── ReportService.java           # JasperReports/POI generation
│   │   │       │
│   │   │       ├── payment/                         # PayMongo integration specifics
│   │   │       │   ├── PayMongoClient.java            # HTTP client wrapper
│   │   │       │   ├── CheckoutSessionRequest.java
│   │   │       │   └── WebhookPayloadParser.java
│   │   │       │
│   │   │       ├── dto/                             # request/response objects
│   │   │       │   ├── CheckoutRequest.java
│   │   │       │   └── ProductForm.java
│   │   │       │
│   │   │       └── exception/
│   │   │           ├── PaymentException.java
│   │   │           └── GlobalExceptionHandler.java   # @ControllerAdvice
│   │   │
│   │   └── resources/
│   │       ├── application.properties               # DB config, PayMongo keys (via env vars)
│   │       │
│   │       ├── templates/                           # Thymeleaf HTML templates
│   │       │   ├── layout/
│   │       │   │   ├── header.html
│   │       │   │   └── footer.html
│   │       │   ├── customer/
│   │       │   │   ├── login.html
│   │       │   │   ├── register.html
│   │       │   │   ├── menu.html
│   │       │   │   ├── cart.html
│   │       │   │   ├── checkout.html
│   │       │   │   └── order_history.html
│   │       │   └── admin/
│   │       │       ├── dashboard.html
│   │       │       ├── product_management.html
│   │       │       ├── order_management.html
│   │       │       ├── user_management.html
│   │       │       └── reports.html
│   │       │
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   ├── theme.css                    # colors, root variables (green/yellow/red)
│   │       │   │   ├── customer.css
│   │       │   │   └── admin.css
│   │       │   ├── js/
│   │       │   │   ├── cart.js
│   │       │   │   └── dashboard-charts.js
│   │       │   └── images/
│   │       │       ├── logo.png
│   │       │       └── products/
│   │       │
│   │       ├── reports/                             # JasperReports .jrxml templates
│   │       │   ├── receipt.jrxml
│   │       │   └── sales_report.jrxml
│   │       │
│   │       └── db/
│   │           └── schema.sql                        # optional manual schema reference
│   │
│   └── test/
│       └── java/
│           └── com/chickenexpress/foodorder/
│               ├── service/
│               │   ├── AuthServiceTest.java
│               │   └── OrderServiceTest.java
│               └── repository/
│                   └── ProductRepositoryTest.java
│
└── target/                          # Maven build output (gitignored)
```

**Architecture rationale:**
- `controller` → `service` → `repository` mirrors standard Spring layered architecture; `entity` replaces the old `model` folder since these are now JPA-managed entities.
- `templates/` and `static/` follow Spring Boot's default Thymeleaf conventions — no manual resource path wiring needed.
- `payment/` stays separate from `service/` since PayMongo integration involves several moving parts (HTTP client, request builders, webhook parsing).
- `api/PayMongoWebhookController` is isolated in its own `controller/api` package since it's a machine-to-machine endpoint, distinct from customer/admin browser-facing controllers.
- `application.properties` should reference DB credentials and the PayMongo secret key via environment variables (e.g., `${DB_PASSWORD}`, `${PAYMONGO_SECRET_KEY}`) rather than hardcoding them, keeping secrets out of source control.

---

## 6. Payment Integration Plan (PayMongo — Webhook-Based)

With a real backend server, PayMongo integration is simpler and more reliable than the desktop polling approach:

1. Customer proceeds to checkout → `PaymentService` calls PayMongo's **Checkout Session** API (POST request with order amount, currency, line items) via `PayMongoClient`
2. PayMongo returns a hosted checkout URL
3. Customer is redirected (standard browser redirect) to PayMongo's hosted checkout page
4. Customer completes payment (card, GCash, GrabPay, Maya)
5. PayMongo sends a **webhook event** to `PayMongoWebhookController` (`/webhooks/paymongo`) when payment succeeds or fails
6. `WebhookPayloadParser` verifies and parses the event, `PaymentService` updates the order status in MySQL accordingly
7. Customer is redirected back to a `success`/`failed` confirmation page on ChickenExpress after PayMongo checkout completes

**Security notes:**
- PayMongo secret key and webhook signing secret are loaded via environment variables, never hardcoded
- Webhook endpoint must verify PayMongo's signature header to prevent spoofed payment confirmations
- For local development, use a tunneling tool (e.g., ngrok) so PayMongo's webhook can reach your local server

---

## 7. Known Limitations & Risks

- Dependency on PayMongo as the sole payment gateway (downtime halts checkout)
- Requires a properly deployed, publicly reachable server for webhooks to work in production (not an issue once hosted, but relevant for local development/demo)
- Simulated/sandbox PayMongo environment recommended for development and defense demo (avoid real transactions)
- Limited development time may constrain testing coverage across all admin/customer flows
- Session/cart handling needs care (server-side session vs. DB-persisted cart) to avoid losing cart contents on server restarts during development

---

## 8. Next Steps for Coding Agent

1. Scaffold a Spring Boot Maven project (Spring Initializr structure) matching Section 5
2. Add dependencies: Spring Web, Spring Data JPA, Spring Security, Thymeleaf, MySQL Driver, Validation
3. Configure `application.properties` for MySQL connection (via environment variables)
4. Define JPA entities (`User`, `Product`, `Category`, `Order`, `OrderItem`, `Payment`) and let Hibernate auto-generate the schema initially, then export a `schema.sql` for reference
5. Implement `SecurityConfig` with role-based access (`/admin/**` restricted to `ROLE_ADMIN`)
6. Build authentication flow (register/login) end-to-end before moving to menu/cart
7. Apply `theme.css` with the color palette from Section 3 across all Thymeleaf templates, layered on Bootstrap 5 or Tailwind
8. Implement product catalog, cart, and checkout flow with a manual "mark as paid" admin fallback for testing before wiring PayMongo
9. Implement PayMongo checkout session creation + webhook endpoint last, once the core ordering flow works end-to-end