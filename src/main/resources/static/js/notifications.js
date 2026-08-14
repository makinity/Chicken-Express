/**
 * ChickenExpress — Real-time Notifications
 *
 * Connects to the STOMP broker via SockJS and subscribes to either:
 *   /topic/admin          — for admin pages
 *   /topic/user/{userId}  — for logged-in customer pages
 *
 * Usage (set before including this script):
 *   <meta name="ws-topic" content="/topic/admin">
 *   OR
 *   <meta name="ws-topic" content="/topic/user/42">
 *
 * Provides:
 *   - Toast popup (bottom-right, auto-dismiss 5 s)
 *   - Bell badge counter (unread count)
 *   - Notification dropdown list (last 30, newest first)
 *   - Badge clears when dropdown is opened
 */
(function () {
    'use strict';

    /* ── Config ──────────────────────────────────────────────────────────── */

    const MAX_STORED   = 30;          // max notifications kept in memory
    const TOAST_DELAY  = 5000;        // ms before toast auto-hides
    const RECONNECT_MS = 5000;        // ms before reconnect attempt

    /* ── State ───────────────────────────────────────────────────────────── */

    let notifications = [];   // { type, title, message, link, icon, at, unread }
    let unreadCount   = 0;

    /* ── DOM refs (set in init) ───────────────────────────────────────────── */

    // Supports both desktop (#ceNotifBadge / #ceNotifList) and
    // mobile (#ceNotifBadgeMobile / #ceNotifListMobile) elements simultaneously
    let bellBadges    = [];   // all badge spans
    let dropdownLists = [];   // all dropdown list uls

    /* ── Colour map by type ─────────────────────────────────────────────── */

    const TYPE_COLOR = {
        NEW_ORDER:          'text-bg-success',
        PAYMENT_CONFIRMED:  'text-bg-success',
        PAYMENT_FAILED:     'text-bg-danger',
        MANUAL_PAID:        'text-bg-success',
        NEW_CUSTOMER:       'text-bg-primary',
        ORDER_CONFIRMED:    'text-bg-success',
        ORDER_PREPARING:    'text-bg-warning',
        ORDER_READY:        'text-bg-info',
        ORDER_COMPLETED:    'text-bg-success',
        ORDER_CANCELLED:    'text-bg-danger',
    };

    function toastColor(type) {
        return TYPE_COLOR[type] || 'text-bg-secondary';
    }

    /* ── STOMP connection ─────────────────────────────────────────────────── */

    function connect() {
        const topicMeta = document.querySelector('meta[name="ws-topic"]');
        if (!topicMeta) return;   // no topic configured for this page — skip

        const topic = topicMeta.content;

        // SockJS + STOMP (loaded from CDN via the layout)
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn('[Notif] SockJS or Stomp not loaded yet — retrying in ' + RECONNECT_MS + 'ms');
            setTimeout(connect, RECONNECT_MS);
            return;
        }

        const socket = new SockJS('/ws');
        const client = Stomp.over(socket);

        // Suppress STOMP debug noise in production
        client.debug = function () {};

        client.connect({}, function onConnected() {
            console.info('[Notif] Connected to ' + topic);

            client.subscribe(topic, function (frame) {
                try {
                    const payload = JSON.parse(frame.body);
                    onNotification(payload);
                } catch (e) {
                    console.error('[Notif] Failed to parse message:', e);
                }
            });
        }, function onError(err) {
            console.warn('[Notif] Connection lost — reconnecting in ' + RECONNECT_MS + 'ms', err);
            setTimeout(connect, RECONNECT_MS);
        });
    }

    /* ── Notification handler ─────────────────────────────────────────────── */

    function onNotification(payload) {
        // Prepend to list
        notifications.unshift({ ...payload, unread: true });
        if (notifications.length > MAX_STORED) {
            notifications = notifications.slice(0, MAX_STORED);
        }

        unreadCount++;
        updateBadge();
        renderDropdown();
        showToast(payload);
    }

    /* ── Badge ───────────────────────────────────────────────────────────── */

    function updateBadge() {
        bellBadges.forEach(function (badge) {
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badge.style.display = '';
            } else {
                badge.style.display = 'none';
            }
        });
    }

    function clearBadge() {
        unreadCount = 0;
        notifications.forEach(function (n) { n.unread = false; });
        updateBadge();
    }

    /* ── Dropdown list ───────────────────────────────────────────────────── */

    function renderDropdown() {
        dropdownLists.forEach(function (dropdownList) {
            if (!dropdownList) return;

            if (notifications.length === 0) {
                dropdownList.innerHTML =
                    '<li class="px-3 py-4 text-center text-muted small">' +
                    '<i class="bi bi-bell-slash d-block fs-3 mb-1 opacity-50"></i>' +
                    'No notifications yet</li>';
                return;
            }

            dropdownList.innerHTML = notifications.map(function (n) {
                const icon      = n.icon  || 'bi-bell';
                const linkOpen  = n.link  ? '<a href="' + n.link + '" class="text-decoration-none text-reset d-block">' : '<div>';
                const linkClose = n.link  ? '</a>' : '</div>';

                return '<li class="border-bottom' + (n.unread ? ' ce-notif-unread' : '') + '">' +
                    linkOpen +
                    '<div class="d-flex align-items-start gap-2 px-3 py-2">' +
                    '<i class="bi ' + icon + ' mt-1 flex-shrink-0 ' +
                        (n.unread ? 'ce-color-primary' : 'text-muted') + '"></i>' +
                    '<div class="flex-grow-1 overflow-hidden">' +
                    '<div class="fw-semibold small lh-sm text-truncate">' + escHtml(n.title) + '</div>' +
                    '<div class="text-muted small mt-1" style="white-space:normal;line-height:1.3;">' +
                        escHtml(n.message) + '</div>' +
                    '<div class="text-muted mt-1" style="font-size:.68rem;">' + escHtml(n.at || '') + '</div>' +
                    '</div></div>' +
                    linkClose + '</li>';
            }).join('');
        });
    }

    /* ── Toast ───────────────────────────────────────────────────────────── */

    function showToast(payload) {
        const container = document.getElementById('ceNotifToastContainer');
        if (!container) return;

        const id    = 'toast-' + Date.now();
        const color = toastColor(payload.type);
        const icon  = payload.icon || 'bi-bell';

        const html =
            '<div id="' + id + '" class="toast align-items-center ' + color +
            ' border-0 shadow" role="alert" aria-live="assertive" aria-atomic="true">' +
            '<div class="d-flex">' +
            '<div class="toast-body d-flex align-items-start gap-2">' +
            '<i class="bi ' + icon + ' mt-1 flex-shrink-0 fs-6"></i>' +
            '<div>' +
            '<div class="fw-semibold small">' + escHtml(payload.title) + '</div>' +
            '<div class="small opacity-90">' + escHtml(payload.message) + '</div>' +
            '</div>' +
            '</div>' +
            '<button type="button" class="btn-close btn-close-white me-2 m-auto" ' +
            'data-bs-dismiss="toast" aria-label="Close"></button>' +
            '</div></div>';

        container.insertAdjacentHTML('beforeend', html);

        const el = document.getElementById(id);
        const toast = new bootstrap.Toast(el, { delay: TOAST_DELAY });

        // If notification has a link, make the toast body clickable
        if (payload.link) {
            el.querySelector('.toast-body').style.cursor = 'pointer';
            el.querySelector('.toast-body').addEventListener('click', function () {
                window.location.href = payload.link;
            });
        }

        toast.show();

        // Remove from DOM after hidden to avoid memory leak
        el.addEventListener('hidden.bs.toast', function () { el.remove(); });
    }

    /* ── HTML escape ─────────────────────────────────────────────────────── */

    function escHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    /* ── Init ────────────────────────────────────────────────────────────── */

    document.addEventListener('DOMContentLoaded', function () {
        // Collect both desktop and mobile elements
        ['ceNotifBadge', 'ceNotifBadgeMobile'].forEach(function (id) {
            const el = document.getElementById(id);
            if (el) bellBadges.push(el);
        });
        ['ceNotifList', 'ceNotifListMobile'].forEach(function (id) {
            const el = document.getElementById(id);
            if (el) dropdownLists.push(el);
        });

        // Clear badge + render dropdown when any bell is opened
        ['ceNotifDropdown', 'ceNotifDropdownMobile'].forEach(function (id) {
            const bellDropdown = document.getElementById(id);
            if (bellDropdown) {
                bellDropdown.addEventListener('show.bs.dropdown', function () {
                    renderDropdown();
                });
                bellDropdown.addEventListener('shown.bs.dropdown', function () {
                    clearBadge();
                });
            }
        });

        // Render initial empty state
        renderDropdown();

        // Start WebSocket connection
        connect();
    });

})();
