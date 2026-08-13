# ChickenExpress

> Web-based chicken food ordering system with PayMongo payment integration.

A Spring Boot web application for a chicken-focused food business with two interfaces:
- **Customer** — browse menu, add to cart, checkout via PayMongo, track orders
- **Admin** — manage products/orders/users, view dashboard analytics, generate Excel/PDF reports

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.2 (Java 21) |
| Database | MySQL + Spring Data JPA (Hibernate) |
| Security | Spring Security (BCrypt, role-based) |
| Templates | Thymeleaf + Bootstrap 5 |
| Payment | PayMongo (hosted checkout + webhooks) |
| Reports | JasperReports (PDF) + Apache POI (Excel) |
| Build | Maven |
| IDE | NetBeans |

---

## Getting Started

### Prerequisites

- Java 21 (LTS)
- Maven 3.8+
- MySQL 8.0+
- NetBeans (or any IDE supporting Maven projects)

### 1. Clone & Set Up Database

```sql
CREATE DATABASE chickenexpress_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Environment Variables

Set the following environment variables (or create `application-local.properties`):

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=chickenexpress_db
DB_USERNAME=root
DB_PASSWORD=your_password

PAYMONGO_SECRET_KEY=sk_test_...
PAYMONGO_PUBLIC_KEY=pk_test_...
PAYMONGO_WEBHOOK_SECRET=whsec_...
APP_BASE_URL=http://localhost:8080
```

### 3. Run

```bash
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/chickenexpress/foodorder/
│   │   ├── config/          # SecurityConfig, WebConfig
│   │   ├── controller/
│   │   │   ├── customer/    # AuthController, MenuController, CartController, ...
│   │   │   ├── admin/       # DashboardController, ProductManagementController, ...
│   │   │   └── api/         # PayMongoWebhookController
│   │   ├── entity/          # JPA entities (User, Product, Order, ...)
│   │   ├── repository/      # Spring Data JPA interfaces
│   │   ├── service/         # Business logic (AuthService, OrderService, ...)
│   │   ├── payment/         # PayMongo integration (PayMongoClient, WebhookPayloadParser)
│   │   ├── dto/             # Form-backing objects (ProductForm, CheckoutRequest)
│   │   └── exception/       # PaymentException, GlobalExceptionHandler
│   └── resources/
│       ├── templates/       # Thymeleaf HTML (layout/, customer/, admin/)
│       ├── static/          # CSS (theme.css, customer.css, admin.css) + JS
│       ├── reports/         # JasperReports .jrxml templates
│       └── db/schema.sql    # MySQL schema reference
└── test/
    └── java/com/chickenexpress/foodorder/
        ├── service/         # AuthServiceTest, OrderServiceTest
        └── repository/      # ProductRepositoryTest
```

---

## Color Palette

| Role | Hex | Usage |
|---|---|---|
| Primary Green | `#639922` | Buttons, active nav, brand |
| Accent Amber | `#EF9F27` | Promo badges, popular tags |
| Danger Red | `#E24B4A` | Errors, spicy tags, delete actions |

---

## Payment Flow (PayMongo)

1. Customer checks out → `PaymentService` creates a PayMongo Checkout Session
2. Customer is redirected to PayMongo's hosted payment page
3. Customer pays (card / GCash / GrabPay / Maya)
4. PayMongo sends a webhook to `/webhooks/paymongo`
5. `WebhookPayloadParser` verifies the signature and parses the event
6. Order status is updated to `PREPARING`
7. Customer is redirected to `/checkout/success`

> For local development, use [ngrok](https://ngrok.com/) to expose your local server for webhooks.

---

## Default Admin Account

After running `schema.sql`, an admin account is seeded:

```
Email:    admin@chickenexpress.com
Password: (set by replacing the BCrypt hash in schema.sql)
```

Generate a new hash:

```java
System.out.println(new BCryptPasswordEncoder().encode("your_password"));
```

---

## Build & Package

```bash
# Run tests
mvn test

# Package as executable jar
mvn package

# Run the jar
java -jar target/foodorder-1.0.0-SNAPSHOT.jar
```

---

## Notes

- PDF reports (`ReportService`) are stubbed — implement after finalizing `.jrxml` layouts
- For production: set `spring.jpa.hibernate.ddl-auto=validate` and manage schema via migrations
- Product images are stored in `uploads/products/` (local filesystem) — configure cloud storage for production
