import {dashboardApi} from '../../apis/dashboardApi.js';

const state = {
    period: 'WEEK'
};

const PERIOD_META = {
    WEEK: {
        summary: 'Thống kê trong 7 ngày gần nhất'
    },
    MONTH: {
        summary: 'Thống kê trong 30 ngày gần nhất'
    },
    QUARTER: {
        summary: 'Thống kê trong 3 tháng gần nhất'
    },
    YEAR: {
        summary: 'Thống kê trong 12 tháng gần nhất'
    }
};

const elements = {
    chartSummary: document.getElementById('donationTrendSummary'),
    chartGrid: document.getElementById('donationTrendGrid'),
    chartCanvas: document.getElementById('donationTrendCanvas'),
    chartBars: document.getElementById('donationTrendBars'),
    chartLabels: document.getElementById('donationTrendLabels'),
    emptyState: document.getElementById('donationTrendEmptyState'),
    periodButtons: document.querySelectorAll('[data-dashboard-period]')
};

const formatCompactMoney = (amount) => {
    const value = Number(amount || 0);
    if (value >= 1000000000) return `${(value / 1000000000).toFixed(1).replace('.0', '')} tỷ`;
    if (value >= 1000000) return `${(value / 1000000).toFixed(1).replace('.0', '')}tr`;
    if (value >= 1000) return `${(value / 1000).toFixed(0)}k`;
    return value.toLocaleString('vi-VN');
};

const formatFullMoney = (amount) => Number(amount || 0).toLocaleString('vi-VN') + ' VNĐ';

const getNiceUpperBound = (value) => {
    if (value <= 0) return 1000000;

    const magnitude = 10 ** Math.floor(Math.log10(value));
    const normalized = value / magnitude;

    if (normalized <= 1) return 1 * magnitude;
    if (normalized <= 2) return 2 * magnitude;
    if (normalized <= 5) return 5 * magnitude;
    return 10 * magnitude;
};

const renderGrid = (maxValue) => {
    const steps = 5;
    const rows = [];

    for (let i = steps; i >= 0; i -= 1) {
        const labelValue = (maxValue / steps) * i;
        rows.push(`
            <div class="flex-1 border-b border-slate-100 flex items-end">
                <span class="inline-block w-14 pr-2 bg-white">${i === 0 ? '0' : formatCompactMoney(labelValue)}</span>
            </div>
        `);
    }

    elements.chartGrid.innerHTML = rows.join('');
};

const renderBars = (points, maxValue) => {
    if (!points.length) {
        elements.chartBars.innerHTML = '';
        elements.chartLabels.innerHTML = '';
        elements.chartGrid.classList.add('hidden');
        elements.chartLabels.classList.add('hidden');
        elements.chartCanvas.classList.add('hidden');
        elements.emptyState.classList.remove('hidden');
        return;
    }

    const hasPositiveValue = points.some((point) => Number(point.totalAmount || 0) > 0);
    elements.emptyState.classList.toggle('hidden', hasPositiveValue);

    if (!hasPositiveValue) {
        elements.chartBars.innerHTML = '';
        elements.chartLabels.innerHTML = '';
        elements.chartGrid.classList.add('hidden');
        elements.chartLabels.classList.add('hidden');
        elements.chartCanvas.classList.add('hidden');
        return;
    }

    elements.chartGrid.classList.remove('hidden');
    elements.chartLabels.classList.remove('hidden');
    elements.chartCanvas.classList.remove('hidden');

    elements.chartBars.innerHTML = points.map((point, index) => {
        const amount = Number(point.totalAmount || 0);
        const percent = maxValue > 0 ? Math.max((amount / maxValue) * 100, amount > 0 ? 6 : 0) : 0;
        const isLatest = index === points.length - 1;
        const shouldShowDefaultTooltip = isLatest && amount > 0;

        return `
            <div class="group relative flex-1 min-w-0 h-full flex items-end">
                <div class="${isLatest ? 'bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20' : 'bg-primary/10 hover:bg-primary/20'} w-full rounded-t-md transition-all duration-300 relative"
                     style="height: ${percent}%">
                    <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 ${shouldShowDefaultTooltip ? 'block' : 'hidden group-hover:block'} bg-slate-900 text-white text-xs ${shouldShowDefaultTooltip ? 'font-bold' : ''} px-2 py-1 rounded whitespace-nowrap z-10">
                        ${formatFullMoney(amount)}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    elements.chartLabels.innerHTML = points.map((point, index) => {
        const isLatest = index === points.length - 1;
        return `<span class="flex-1 min-w-0 text-center ${isLatest ? 'font-bold text-primary' : ''}">${point.label}</span>`;
    }).join('');
};

const updatePeriodUI = () => {
    const meta = PERIOD_META[state.period] || PERIOD_META.WEEK;
    elements.chartSummary.textContent = meta.summary;

    elements.periodButtons.forEach((button) => {
        const isActive = button.dataset.dashboardPeriod === state.period;
        button.className = isActive
            ? 'rounded-lg border border-primary bg-primary px-3 py-2 text-xs font-semibold text-white transition-colors'
            : 'rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition-colors';
    });
};

const loadDonationTrend = async () => {
    try {
        updatePeriodUI();
        const response = await dashboardApi.getDonationTrend({period: state.period});
        const points = response?.data?.points || [];
        const amounts = points.map((point) => Number(point.totalAmount || 0));
        const maxValue = getNiceUpperBound(Math.max(...amounts, 0));

        renderGrid(maxValue);
        renderBars(points, maxValue);
    } catch (error) {
        console.error('Không thể tải dữ liệu xu hướng quyên góp:', error);
        elements.emptyState.classList.remove('hidden');
        elements.emptyState.textContent = 'Không thể tải dữ liệu xu hướng quyên góp.';
        elements.chartBars.innerHTML = '';
        elements.chartLabels.innerHTML = '';
        elements.chartGrid.classList.add('hidden');
        elements.chartLabels.classList.add('hidden');
        elements.chartCanvas.classList.add('hidden');
        renderGrid(getNiceUpperBound(0));
    }
};

const bindEvents = () => {
    elements.periodButtons.forEach((button) => {
        button.addEventListener('click', () => {
            const nextPeriod = button.dataset.dashboardPeriod;
            if (!nextPeriod || nextPeriod === state.period) return;
            state.period = nextPeriod;
            loadDonationTrend();
        });
    });
};

document.addEventListener('DOMContentLoaded', () => {
    if (!elements.chartBars || !elements.chartGrid || !elements.chartLabels) return;
    bindEvents();
    loadDonationTrend();
});
