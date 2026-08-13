package com.chickenexpress.foodorder.exception;

/**
 * Thrown when a PayMongo API call fails, a webhook signature is invalid,
 * or any other payment-related error occurs.
 *
 * Caught by GlobalExceptionHandler to return an appropriate error response.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
