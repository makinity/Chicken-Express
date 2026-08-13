# ChickenExpress — Test Users

## Accounts

| Name | Email | Password | Role |
|------|-------|----------|------|
| Admin | admin@chickenexpress.com | admin123 | ROLE_ADMIN |
| Maria Santos | maria@example.com | customer123 | ROLE_CUSTOMER |
| Jose Reyes | jose@example.com | customer123 | ROLE_CUSTOMER |
| Ana Garcia | ana@example.com | customer123 | ROLE_CUSTOMER |
| Carlo Mendoza | carlo@example.com | customer123 | ROLE_CUSTOMER |
| Lea Villanueva | lea@example.com | customer123 | ROLE_CUSTOMER |
| Marco Bautista | marco@example.com | customer123 | ROLE_CUSTOMER |

> All accounts are seeded by `DataInitializer` on first startup (skipped if products already exist).

---

## Seeded Data Summary

| Entity | Count |
|--------|-------|
| Categories | 5 |
| Products | 26 |
| Users | 7 (1 admin, 6 customers) |
| Orders | 28 (spread across last 7 days) |

### Categories
1. Chicken Meals
2. Combos & Bundles
3. Sides
4. Drinks
5. Desserts

---

## Pages

| Page | URL | Access |
|------|-----|--------|
| Home | http://localhost:8080/ | Public |
| Menu | http://localhost:8080/menu | Public |
| Login | http://localhost:8080/login | Public |
| Register | http://localhost:8080/register | Public |
| Cart | http://localhost:8080/cart | ROLE_CUSTOMER |
| Orders | http://localhost:8080/orders | ROLE_CUSTOMER |
| Admin Dashboard | http://localhost:8080/admin/dashboard | ROLE_ADMIN |
| Admin Products | http://localhost:8080/admin/products | ROLE_ADMIN |
| Admin Orders | http://localhost:8080/admin/orders | ROLE_ADMIN |
| Admin Categories | http://localhost:8080/admin/categories | ROLE_ADMIN |
| Admin Users | http://localhost:8080/admin/users | ROLE_ADMIN |
