# ChickenExpress — Notification System Plan

## Overview

Real-time notifications via **WebSocket + STOMP** (Spring WebSocket).  
Two audiences: **Admin** and **Customer**.  
Strategy: **fire-and-forget** (no DB persistence for now — can be added later).  
SockJS fallback enabled for browser/proxy compatibility.

---

## Architecture

```
Action (Service / Webhook)
        │
        ▼
NotificationService.send(...)
        │
        ├─▶  /topic/admin          ← all connected admins receive it
        │
        └─▶  /topic/user/{userId}  ← only that customer receives it
```

- Admin notifications → broadcast to `/topic/admin`  
- Customer notifications → user-specific to `/topic/user/{userId}`  
- Each notification is a small JSON payload:

```json
{
  "type":    "NEW_ORDER",
  "title":   "New Order",
  "message": "CE-20260814-0012 placed by Maki — ₱450.00",
  "link":    "/admin/orders/42",
  "icon":    "bi-receipt",
  "at":      "2026-08-14T11:06:00"
}
```

---

## Notification Events

### 🔴 Admin Notifications  
> Sent to `/topic/admin` — visible to all connected admin sessions.

| # | Event | Trigger point | Title | Message |
|---|-------|--------------|-------|---------|
| A1 | **New order placed** | `OrderService.placeOrder()` — after save | 🛒 New Order | `{orderNumber}` by `{customerName}` — `₱{total}` |
| A2 | **Payment confirmed** | `PaymentService.handleWebhook()` — `checkout_session.payment.paid` | 💳 Payment Received | `{orderNumber}` paid via `{method}` — `₱{amount}` |
| A3 | **Payment failed** | `PaymentService.handleWebhook()` — `checkout_session.payment.failed` | ❌ Payment Failed | `{orderNumber}` payment failed — order cancelled |
| A4 | **Order marked paid manually** | `PaymentService.markAsPaid()` | ✅ Marked as Paid | `{orderNumber}` manually marked as paid |
| A5 | **New customer registered** | `AuthService.register()` — after save | 👤 New Customer | `{fullName}` (`{email}`) just registered |

---

### 🟢 Customer Notifications  
> Sent to `/topic/user/{userId}` — private to the customer who placed the order.

| # | Event | Trigger point | Title | Message |
|---|-------|--------------|-------|---------|
| C1 | **Order confirmed** | `OrderService.placeOrder()` — after save | ✅ Order Confirmed | Your order `{orderNumber}` has been received! |
| C2 | **Payment confirmed** | `PaymentService.handleWebhook()` — `checkout_session.payment.paid` | 💳 Payment Received | Payment for `{orderNumber}` confirmed. We're preparing it now! |
| C3 | **Payment failed** | `PaymentService.handleWebhook()` — `checkout_session.payment.failed` | ❌ Payment Failed | Your payment for `{orderNumber}` failed. Please try again. |
| C4 | **Order is being prepared** | `OrderService.updateStatus()` → `PREPARING` | 👨‍🍳 Being Prepared | Your order `{orderNumber}` is now being prepared! |
| C5 | **Order is ready** | `OrderService.updateStatus()` → `READY` | 🍗 Order Ready! | Your order `{orderNumber}` is ready for pickup! |
| C6 | **Order completed** | `OrderService.updateStatus()` → `COMPLETED` | 🎉 Order Completed | Thank you! `{orderNumber}` has been completed. |
| C7 | **Order cancelled** | `OrderService.updateStatus()` → `CANCELLED` | ❌ Order Cancelled | Your order `{orderNumber}` has been cancelled. |

---

## Files to Create / Modify

### New files

| File | Purpose |
|------|---------|
| `config/WebSocketConfig.java` | Enable STOMP, register `/ws` endpoint with SockJS, set `/topic` broker prefix |
| `service/NotificationService.java` | Wrapper around `SimpMessagingTemplate` — `sendToAdmin()` and `sendToUser(userId, ...)` |
| `dto/NotificationPayload.java` | The JSON payload record (`type`, `title`, `message`, `link`, `icon`, `at`) |
| `static/js/notifications.js` | SockJS + STOMP client, subscribe logic, toast renderer, badge counter |
| `templates/fragments/notifications-script.html` | Thymeleaf fragment wrapping the JS include (easier to conditionally include per layout) |

### Modified files

| File | Change |
|------|--------|
| `service/OrderService.java` | Inject `NotificationService`, call after `placeOrder()` and after `updateStatus()` |
| `service/PaymentService.java` | Inject `NotificationService`, call after webhook paid/failed and after `markAsPaid()` |
| `service/AuthService.java` | Inject `NotificationService`, call after `register()` |
| `templates/layout/admin-layout.html` | Add notification bell icon + unread badge + include `notifications.js` subscribed to `/topic/admin` |
| `templates/layout/header.html` | Add notification bell for logged-in customers + include `notifications.js` subscribed to `/topic/user/{userId}` |
| `config/SecurityConfig.java` | Allow `/ws/**` without auth (SockJS handshake uses a public HTTP upgrade) |
| `pom.xml` | Add `spring-boot-starter-websocket` dependency |

---

## WebSocket Topic Map

```
/ws                          ← SockJS / STOMP connection endpoint
/topic/admin                 ← broadcast to all admins
/topic/user/{userId}         ← private per-customer channel
```

---

## UI Behaviour

### Admin (topbar bell)
- Bell icon in the topbar with a red badge showing unread count
- Clicking the bell opens a dropdown listing recent notifications (last 20, in-memory)
- Each notification has: icon · title · message · timestamp · optional link
- Clicking a notification navigates to the relevant page (e.g. `/admin/orders/42`)
- Badge clears when the dropdown is opened (mark-as-read, client-side only)
- Toast popup in bottom-right for each incoming notification (auto-dismiss 5s)

### Customer (top navbar)
- Bell icon in the customer navbar (only shown when logged in)
- Same dropdown + toast pattern
- Notifications are order-status updates specific to that user
- Toast for C2 ("Payment confirmed") links to `/orders/{orderId}`

---

## Notification Type Constants

```
NEW_ORDER           → A1
PAYMENT_CONFIRMED   → A2, C2
PAYMENT_FAILED      → A3, C3
MANUAL_PAID         → A4
NEW_CUSTOMER        → A5
ORDER_CONFIRMED     → C1
ORDER_PREPARING     → C4
ORDER_READY         → C5
ORDER_COMPLETED     → C6
ORDER_CANCELLED     → C7
```

---

## Implementation Order

1. `pom.xml` — add `spring-boot-starter-websocket`
2. `NotificationPayload.java` — DTO record
3. `WebSocketConfig.java` — STOMP broker config
4. `SecurityConfig.java` — allow `/ws/**`
5. `NotificationService.java` — send helpers
6. Hook into `OrderService`, `PaymentService`, `AuthService`
7. `notifications.js` — client-side STOMP + toast + badge
8. Update `admin-layout.html` — bell + badge
9. Update `header.html` — bell + badge for customers

---

## Future Enhancements (out of scope for now)

- Persist notifications to a `notifications` DB table with `read_at` column
- Load unread count from DB on page load (so count survives refresh)
- Mark individual notifications as read
- Per-user notification preferences (opt in/out per event type)
- Email/SMS notifications for `ORDER_READY` (via SendGrid / Twilio)
