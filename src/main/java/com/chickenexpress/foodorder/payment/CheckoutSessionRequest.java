package com.chickenexpress.foodorder.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps the PayMongo Checkout Session API request body structure.
 *
 * Used for documentation and type-safety reference.
 * PayMongoClient builds the actual JSON manually via ObjectMapper.
 *
 * Reference: https://developers.paymongo.com/reference/create-a-checkout-session
 */
public class CheckoutSessionRequest {

    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    // ── Inner Classes ────────────────────────────────────────────────────────

    public static class Data {
        private Attributes attributes;

        public Attributes getAttributes() { return attributes; }
        public void setAttributes(Attributes attributes) { this.attributes = attributes; }
    }

    public static class Attributes {

        @JsonProperty("billing")
        private Billing billing;

        @JsonProperty("line_items")
        private List<LineItem> lineItems;

        @JsonProperty("payment_method_types")
        private List<String> paymentMethodTypes;

        @JsonProperty("success_url")
        private String successUrl;

        @JsonProperty("cancel_url")
        private String cancelUrl;

        @JsonProperty("description")
        private String description;

        @JsonProperty("reference_number")
        private String referenceNumber;

        // Getters & Setters

        public Billing getBilling() { return billing; }
        public void setBilling(Billing billing) { this.billing = billing; }

        public List<LineItem> getLineItems() { return lineItems; }
        public void setLineItems(List<LineItem> lineItems) { this.lineItems = lineItems; }

        public List<String> getPaymentMethodTypes() { return paymentMethodTypes; }
        public void setPaymentMethodTypes(List<String> types) { this.paymentMethodTypes = types; }

        public String getSuccessUrl() { return successUrl; }
        public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }

        public String getCancelUrl() { return cancelUrl; }
        public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    }

    public static class LineItem {
        private String currency;
        private long amount;      // centavos
        private String name;
        private int quantity;

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class Billing {
        private String name;
        private String email;
        private String phone;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
