-- =============================================================================
--  ChickenExpress — schema.sql
--  MySQL schema reference (manually maintained).
--
--  In development, Hibernate auto-generates this schema via ddl-auto=update.
--  This file serves as:
--    1. A readable reference of the intended DB structure
--    2. A starting point for production migrations
--    3. A way to create the DB from scratch without Hibernate
--
--  Run against a fresh MySQL database:
--    CREATE DATABASE chickenexpress_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--    USE chickenexpress_db;
--    SOURCE schema.sql;
-- =============================================================================

-- ── Categories ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Users ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_CUSTOMER',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    phone       VARCHAR(20),
    address     VARCHAR(255),
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Products ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)    NOT NULL,
    description VARCHAR(500),
    price       DECIMAL(10, 2)  NOT NULL,
    image_url   VARCHAR(255),
    available   TINYINT(1)      NOT NULL DEFAULT 1,
    popular     TINYINT(1)      NOT NULL DEFAULT 0,
    spicy       TINYINT(1)      NOT NULL DEFAULT 0,
    category_id BIGINT          NOT NULL,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    KEY idx_product_category (category_id),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Orders ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    order_number     VARCHAR(30)     NOT NULL,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    order_type       VARCHAR(20)              DEFAULT 'TAKEOUT',
    total_amount     DECIMAL(10, 2)  NOT NULL,
    notes            VARCHAR(500),
    delivery_address VARCHAR(255),
    user_id          BIGINT          NOT NULL,
    created_at       DATETIME        NOT NULL,
    updated_at       DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_number (order_number),
    KEY idx_order_user (user_id),
    KEY idx_order_status (status),
    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Order Items ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    quantity    INT             NOT NULL,
    unit_price  DECIMAL(10, 2)  NOT NULL,
    subtotal    DECIMAL(10, 2)  NOT NULL,
    order_id    BIGINT          NOT NULL,
    product_id  BIGINT          NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_item_order   (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)   REFERENCES orders   (id),
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Cart Items ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT     NOT NULL AUTO_INCREMENT,
    quantity    INT        NOT NULL,
    added_at    DATETIME   NOT NULL,
    user_id     BIGINT     NOT NULL,
    product_id  BIGINT     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cart_user_product (user_id, product_id),
    KEY idx_cart_user (user_id),
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id)    REFERENCES users    (id),
    CONSTRAINT fk_cart_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Payments ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payments (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    paymongo_session_id   VARCHAR(100),
    paymongo_payment_id   VARCHAR(100),
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    amount                DECIMAL(10, 2)  NOT NULL,
    currency              VARCHAR(10)              DEFAULT 'PHP',
    payment_method        VARCHAR(50),
    webhook_event_type    VARCHAR(100),
    created_at            DATETIME        NOT NULL,
    paid_at               DATETIME,
    order_id              BIGINT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payment_order    (order_id),
    KEY idx_payment_session        (paymongo_session_id),
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
--  Sample seed data (for development / demo)
--  Remove or replace before deploying to production.
-- =============================================================================

-- Categories
INSERT IGNORE INTO categories (name, description, sort_order) VALUES
    ('Chicken Meals',    'Fried, grilled, and spicy chicken',          1),
    ('Combos / Bundles', 'Chicken + rice + drink combos, family meals', 2),
    ('Sides',            'Gravy, fries, coleslaw, mashed potato',       3),
    ('Drinks',           'Iced tea, soda, water, juice',                4),
    ('Desserts',         'Ice cream, halo-halo, brownies',              5);

-- Admin user
-- username : admin@chickenexpress.com
-- password : password
-- Hash generated with BCryptPasswordEncoder (cost factor 10)
INSERT IGNORE INTO users (full_name, email, password, role, enabled, created_at)
VALUES ('Admin', 'admin@chickenexpress.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ROLE_ADMIN', 1, NOW());
