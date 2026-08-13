package com.chickenexpress.foodorder.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for browser-facing (Thymeleaf) controllers.
 *
 * Renders a user-friendly error page instead of the default white-label error page.
 * API endpoints (webhooks) catch their own exceptions and return HTTP status codes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", "The page you're looking for doesn't exist.");
        return "error";
    }

    // ── 403 Access Denied ────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("message", "You don't have permission to access this page.");
        return "error";
    }

    // ── Payment Errors ───────────────────────────────────────────────────────

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handlePaymentException(PaymentException ex, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("message", "A payment error occurred. Please try again or contact support.");
        return "error";
    }

    // ── Illegal Argument (entity not found, bad input) ───────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    // ── Generic Fallback ─────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("message", "Something went wrong. Please try again later.");
        return "error";
    }
}
