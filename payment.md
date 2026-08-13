# PayMongo Payment Integration Guide

> **Status:** Code is ready. Keys are not yet configured. Follow this guide step-by-step before testing payments.

---

## Overview

ChickenExpress uses **PayMongo Checkout Sessions** — a hosted payment page where customers are redirected to complete their payment. We never touch raw card numbers. PayMongo handles the PCI compliance.

**Accepted payment methods:** Visa/Mastercard, GCash, GrabPay, Maya

**Flow summary:**

```
Customer clicks "Pay with PayMongo"
        ↓
POST /checkout  (CheckoutController)
        ↓
OrderService.placeOrder()  →  creates Order (PENDING)
        ↓
PaymentService.initiateCheckout()  →  creates Payment (PENDING)
        ↓
PayMongoClient.createCheckoutSession()  →  POST https://api.paymongo.com/v1/checkout_sessions
        ↓
PayMongo returns checkout_url
        ↓
Customer is redirected to PayMongo's hosted page
        ↓
Customer pays  →  PayMongo calls POST /webhooks/paymongo
        ↓
WebhookPayloadParser.verifySignature()  +  parse()
        ↓
PaymentService.handleWebhook()
        →  Payment.status = PAID
        →  Order.status   = PREPARING
        ↓
Customer is redirected to /checkout/success
```

---

## Step 1: Create a PayMongo Account

1. Go to [https://dashboard.paymongo.com](https://dashboard.paymongo.com) and sign up.
2. Complete business verification to unlock live mode (test mode works immediately).
3. Once logged in, go to **Developers → API Keys**.

---

## Step 2: Get Your API Keys

From the PayMongo Dashboard → **Developers → API Keys**, copy:

| Key | Prefix | Used For |
|---|---|---|
| Secret Key | `sk_test_...` | Server-side API calls (never expose to browser) |
| Public Key | `pk_test_...` | Future client-side use (not used yet in this app) |

> For production, use `sk_live_...` and `pk_live_...` keys.

---

## Step 3: Set Up the Webhook

PayMongo needs a public URL to send payment confirmations to. Our endpoint is:

```
POST /webhooks/paymongo
```

### For Local Development (ngrok)

PayMongo cannot reach `localhost`. Use ngrok to create a tunnel:

```bash
# Install ngrok: https://ngrok.com/download
ngrok http 8080
```

Ngrok gives you a URL like `https://abc123.ngrok-free.app`. Your webhook URL is:

```
https://abc123.ngrok-free.app/webhooks/paymongo
```

**Note:** The ngrok URL changes every restart (on the free plan). You'll need to update the webhook URL in the PayMongo dashboard each time.

### Register the Webhook in the Dashboard

1. Go to **Developers → Webhooks → Add Endpoint**
2. Set the URL to your ngrok URL + `/webhooks/paymongo`
3. Select event: **`checkout_session.payment.paid`** and **`checkout_session.payment.failed`**
4. Click **Create**
5. Copy the **Webhook Secret** shown (starts with `whsec_...`) — you only see it once

### For Production

Set `APP_BASE_URL` to your real domain (e.g., `https://chickenexpress.com`). The webhook URL becomes:

```
https://chickenexpress.com/webhooks/paymongo
```

---

## Step 4: Configure Environment Variables

Set these before running the app. **Never hardcode these values in source code.**

### Option A: System Environment Variables (Recommended)

On Windows (PowerShell):
```powershell
$env:PAYMONGO_SECRET_KEY   = "sk_test_your_key_here"
$env:PAYMONGO_PUBLIC_KEY   = "pk_test_your_key_here"
$env:PAYMONGO_WEBHOOK_SECRET = "whsec_your_secret_here"
$env:APP_BASE_URL           = "http://localhost:8080"
$env:DB_HOST     = "localhost"
$env:DB_PORT     = "3306"
$env:DB_NAME     = "chickenexpress_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_db_password"
```

### Option B: `.env` File (Already in Project)

The `.env` file at the project root is **gitignored**. Fill in the values:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=chickenexpress_db
DB_USERNAME=root
DB_PASSWORD=your_db_password

PAYMONGO_SECRET_KEY=sk_test_your_key_here
PAYMONGO_PUBLIC_KEY=pk_test_your_key_here
PAYMONGO_WEBHOOK_SECRET=whsec_your_secret_here
APP_BASE_URL=http://localhost:8080
```

> Spring Boot does not auto-load `.env` files. You need to either source it manually or use a plugin. The environment variable approach (Option A) is the simplest for local development.

### How These Map to `application.properties`

```properties
# Already configured — don't change these lines
paymongo.secret-key=${PAYMONGO_SECRET_KEY:}
paymongo.public-key=${PAYMONGO_PUBLIC_KEY:}
paymongo.webhook-secret=${PAYMONGO_WEBHOOK_SECRET:}
paymongo.base-url=https://api.paymongo.com/v1
paymongo.success-url=${APP_BASE_URL:http://localhost:8080}/checkout/success
paymongo.cancel-url=${APP_BASE_URL:http://localhost:8080}/checkout/cancel
```

---

## Step 5: Test the Full Flow

### PayMongo Test Cards

Use these card numbers on the PayMongo hosted payment page:

| Scenario | Card Number | Expiry | CVV |
|---|---|---|---|
| Successful payment | `4343 4343 4343 4345` | Any future date | Any 3 digits |
| Failed payment | `4111 1111 1111 1111` | Any future date | Any 3 digits |

For **GCash / GrabPay / Maya**: PayMongo's test mode shows a simulated page — just click "Authorize" to simulate a successful payment.

### Manual Test Checklist

- [ ] Start ngrok: `ngrok http 8080`
- [ ] Update webhook URL in PayMongo dashboard with new ngrok URL
- [ ] Set environment variables with your test keys
- [ ] Run the app: `mvn spring-boot:run`
- [ ] Log in as a customer, add items to cart
- [ ] Go to `/checkout`, click **Pay with PayMongo**
- [ ] Complete payment on the PayMongo hosted page
- [ ] You should be redirected to `/checkout/success`
- [ ] Check the database: `payments.status` should be `PAID`, `orders.status` should be `PREPARING`
- [ ] Check the admin dashboard — the order should appear as **Preparing**

---

## Code Reference

All payment code is already written. Here's where everything lives:

### Classes

| File | Responsibility |
|---|---|
| `payment/PayMongoClient.java` | Makes HTTP calls to `POST /v1/checkout_sessions`. Builds line items, handles auth header (Basic + Base64 secret key), parses the response. |
| `payment/WebhookPayloadParser.java` | Verifies the `Paymongo-Signature` HMAC-SHA256 header. Parses the webhook JSON into a `WebhookEvent` record. |
| `payment/CheckoutSessionRequest.java` | Type-safe model of the PayMongo Checkout Session request body (for reference). |
| `service/PaymentService.java` | Orchestrates the flow: creates the `Payment` entity, calls `PayMongoClient`, handles webhook routing, updates order status. |
| `controller/customer/CheckoutController.java` | Handles `POST /checkout` — places the order, calls `PaymentService.initiateCheckout()`, redirects to PayMongo URL. |
| `controller/api/PayMongoWebhookController.java` | Receives `POST /webhooks/paymongo`, validates the signature header is present, delegates to `PaymentService.handleWebhook()`. |
| `entity/Payment.java` | Database record: stores session ID, payment ID, status, method, timestamps. One-to-one with `Order`. |
| `entity/Order.java` | Status updated to `PREPARING` on successful payment webhook. |

### Key Method Signatures

```java
// Initiate checkout — call this when the order is placed
// Returns the URL to redirect the customer to
String PaymentService.initiateCheckout(Order order)

// Handle incoming webhook — call this in the webhook controller
// Verifies signature + updates Order/Payment status
void PaymentService.handleWebhook(String rawBody, String signature)

// Admin fallback — mark an order paid without going through PayMongo
void PaymentService.markAsPaid(Long orderId)
```

### Payment Status Values (`Payment.Status`)

| Value | Meaning |
|---|---|
| `PENDING` | Checkout session created, customer hasn't paid yet |
| `PAID` | Webhook confirmed successful payment |
| `FAILED` | Payment failed or checkout session expired |
| `REFUNDED` | Manually recorded after processing a refund |

### Order Status After Payment

| Payment Event | `Payment.status` | `Order.status` |
|---|---|---|
| `checkout_session.payment.paid` | `PAID` | `PREPARING` |
| `checkout_session.payment.failed` | `FAILED` | `CANCELLED` |

---

## Security Notes

- **Secret key is server-side only.** It is never sent to the browser or included in templates.
- **Webhook signature verification** is done in `WebhookPayloadParser.verifySignature()` using HMAC-SHA256 before any payload is trusted.
- **CSRF is disabled for `/webhooks/paymongo`** in `SecurityConfig` — this is intentional and safe because PayMongo is a machine caller, not a browser. Security is enforced by the signature header instead.
- **The webhook endpoint requires no authentication** — again intentional and safe for the same reason.
- Never commit real API keys to Git. The `.env` file and environment variables are the correct approach.

---

## Going Live (Production Checklist)

Before switching from test to live mode:

- [ ] Complete PayMongo business verification
- [ ] Replace `sk_test_...` with `sk_live_...` in environment variables
- [ ] Replace `whsec_...` with the live webhook secret
- [ ] Register the production webhook URL (your real domain) in the PayMongo dashboard
- [ ] Set `APP_BASE_URL` to your production domain
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` in production properties
- [ ] Test with a real ₱1 transaction before opening to customers

---

## Troubleshooting

**`PaymentException: PayMongo API error 401`**
→ Secret key is wrong or empty. Check your `PAYMONGO_SECRET_KEY` environment variable.

**`PaymentException: Webhook signature verification failed`**
→ Webhook secret doesn't match. Regenerate it in the PayMongo dashboard and update `PAYMONGO_WEBHOOK_SECRET`.

**Webhook not being received at all**
→ ngrok is not running, or the webhook URL in the PayMongo dashboard is stale. Restart ngrok and update the URL.

**Order stays `PENDING` after payment**
→ The webhook was not received or failed to process. Check the app logs for exceptions. Also check the PayMongo dashboard under **Developers → Webhooks → Event Logs** to see delivery attempts.

**`PaymentException: No payment record found for session: cs_...`**
→ The `payments` table doesn't have a row matching the session ID. This usually means `initiateCheckout()` failed silently before saving the session ID. Check logs around the checkout POST.

**Customer redirected to `/checkout/cancel` immediately**
→ The checkout session expired (PayMongo sessions expire after 1 hour by default). This is expected behavior — the customer just needs to start over.
