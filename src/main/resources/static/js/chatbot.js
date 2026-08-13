/**
 * ChickenExpress — Chatbot UI
 * Handles open/close toggle, message sending, and response rendering.
 */
(function () {
    'use strict';

    const btn       = document.getElementById('ce-chatbot-btn');
    const container = document.getElementById('ce-chatbot-container');
    const closeBtn  = document.getElementById('ce-chatbot-close');
    const form      = document.getElementById('ce-chatbot-form');
    const input     = document.getElementById('ce-chatbot-input');
    const messages  = document.getElementById('ce-chatbot-messages');

    if (!btn || !container) return;

    // ── Toggle open / close ─────────────────────────────────────────────────

    btn.addEventListener('click', () => {
        const isOpen = container.classList.toggle('ce-chatbot-open');
        btn.setAttribute('aria-expanded', isOpen);
        if (isOpen) {
            input.focus();
            // Show welcome message on first open
            if (messages.children.length === 0) {
                appendMessage('bot',
                    '👋 Hi! I\'m the ChickenExpress assistant. Ask me anything about our menu, promos, or orders!');
            }
        }
    });

    closeBtn.addEventListener('click', () => {
        container.classList.remove('ce-chatbot-open');
        btn.setAttribute('aria-expanded', false);
    });

    // Close on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && container.classList.contains('ce-chatbot-open')) {
            container.classList.remove('ce-chatbot-open');
            btn.setAttribute('aria-expanded', false);
        }
    });

    // ── Send message ────────────────────────────────────────────────────────

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const text = input.value.trim();
        if (!text) return;

        appendMessage('user', text);
        input.value = '';
        input.disabled = true;

        const typing = appendTyping();

        try {
            const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

            const res = await fetch('/api/chat', {
                method: 'POST',
                headers,
                body: JSON.stringify({ message: text })
            });

            const data = await res.json();
            typing.remove();
            appendMessage('bot', data.reply || data.error || 'No response received.');

        } catch (err) {
            typing.remove();
            appendMessage('bot', 'Sorry, something went wrong. Please try again.');
        } finally {
            input.disabled = false;
            input.focus();
        }
    });

    // ── Helpers ─────────────────────────────────────────────────────────────

    function appendMessage(role, text) {
        const div = document.createElement('div');
        div.className = `ce-chat-msg ce-chat-msg--${role}`;

        const bubble = document.createElement('div');
        bubble.className = 'ce-chat-bubble';
        bubble.textContent = text;

        div.appendChild(bubble);
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
        return div;
    }

    function appendTyping() {
        const div = document.createElement('div');
        div.className = 'ce-chat-msg ce-chat-msg--bot';
        div.innerHTML = `
            <div class="ce-chat-bubble ce-chat-typing">
                <span></span><span></span><span></span>
            </div>`;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
        return div;
    }

})();
