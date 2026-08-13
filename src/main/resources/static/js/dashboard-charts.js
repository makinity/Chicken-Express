/**
 * ChickenExpress — dashboard-charts.js
 *
 * Initializes Chart.js charts on the admin dashboard.
 *
 * Charts are only rendered when the corresponding canvas elements exist.
 * Data is expected to be embedded in the page as JSON inside
 * <script type="application/json"> tags (server-rendered by Thymeleaf).
 *
 * Chart.js CDN must be loaded before this script.
 * Add to dashboard.html: <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
 *
 * Usage (in dashboard.html):
 *   <canvas id="salesChart" width="400" height="200"></canvas>
 *   <script type="application/json" id="salesData">
 *     { "labels": ["Mon","Tue","Wed"], "values": [1200, 980, 1500] }
 *   </script>
 */

document.addEventListener('DOMContentLoaded', () => {

    // ── Helper: parse embedded JSON data ────────────────────────────────

    function getChartData(elementId) {
        const el = document.getElementById(elementId);
        if (!el) return null;
        try {
            return JSON.parse(el.textContent);
        } catch (e) {
            console.warn('Failed to parse chart data for', elementId, e);
            return null;
        }
    }

    // ── ChickenExpress brand colors ──────────────────────────────────────

    const primaryColor = '#639922';
    const accentColor  = '#EF9F27';
    const dangerColor  = '#E24B4A';

    // ── Sales Over Time (line chart) ─────────────────────────────────────

    const salesCtx = document.getElementById('salesChart');
    if (salesCtx) {
        const data = getChartData('salesData');
        if (data) {
            new Chart(salesCtx, {
                type: 'line',
                data: {
                    labels: data.labels,
                    datasets: [{
                        label: 'Sales (₱)',
                        data: data.values,
                        borderColor: primaryColor,
                        backgroundColor: 'rgba(99, 153, 34, 0.10)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.3,
                        pointBackgroundColor: primaryColor,
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: ctx => '₱' + ctx.parsed.y.toFixed(2)
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: val => '₱' + val
                            }
                        }
                    }
                }
            });
        }
    }

    // ── Orders by Status (doughnut chart) ────────────────────────────────

    const statusCtx = document.getElementById('statusChart');
    if (statusCtx) {
        const data = getChartData('statusData');
        if (data) {
            new Chart(statusCtx, {
                type: 'doughnut',
                data: {
                    labels: data.labels,
                    datasets: [{
                        data: data.values,
                        backgroundColor: [
                            '#6c757d',   // PENDING
                            accentColor, // PREPARING
                            '#0dcaf0',   // READY
                            primaryColor,// COMPLETED
                            dangerColor  // CANCELLED
                        ],
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { position: 'bottom' }
                    }
                }
            });
        }
    }

    // ── Top Products (horizontal bar chart) ──────────────────────────────

    const topProductsCtx = document.getElementById('topProductsChart');
    if (topProductsCtx) {
        const data = getChartData('topProductsData');
        if (data) {
            new Chart(topProductsCtx, {
                type: 'bar',
                data: {
                    labels: data.labels,
                    datasets: [{
                        label: 'Units Sold',
                        data: data.values,
                        backgroundColor: primaryColor,
                        borderRadius: 4,
                    }]
                },
                options: {
                    indexAxis: 'y',
                    responsive: true,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        x: { beginAtZero: true }
                    }
                }
            });
        }
    }

});
