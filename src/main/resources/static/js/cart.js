/**
 * ChickenExpress — cart.js
 *
 * Handles client-side cart interactions:
 * - Debounced quantity updates (auto-submit on change)
 * - Real-time subtotal calculation on the cart page
 * - "Add to cart" success toast notifications
 */

document.addEventListener('DOMContentLoaded', () => {

    // ── Quantity inputs — auto-submit with debounce ──────────────────────

    let debounceTimer = null;

    document.querySelectorAll('input[name="quantity"]').forEach(input => {
        input.addEventListener('change', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                input.closest('form').submit();
            }, 400);
        });

        // Prevent form submit on Enter (avoid accidental double-submit)
        input.addEventListener('keydown', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                input.blur();
            }
        });
    });

    // ── Bootstrap toast for "added to cart" flash message ───────────────

    const flashMessage = document.querySelector('[data-ce-toast]');
    if (flashMessage) {
        const toast = new bootstrap.Toast(flashMessage, { delay: 3000 });
        toast.show();
    }

    // ── Update cart item count in the navbar badge dynamically ──────────
    // (Only needed for AJAX-based add-to-cart. For now, page reloads handle this.)
    // Placeholder for future enhancement.

});
