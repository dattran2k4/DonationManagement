import {donorApi} from '../../apis/donorApi.js';
import {renderPagination} from '../../components/pagination.js';

const donorId = window.__DONOR_ID__;
const state = {
    page: 1,
    size: 10
};

const elements = {
    tableBody: document.getElementById('donorDonationTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

const formatCurrency = (amount) => `${new Intl.NumberFormat('vi-VN').format(amount || 0)} đ`;

const formatDateTime = (dateTime) => {
    if (!dateTime) return '---';
    return new Date(dateTime).toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
};

const getStatusBadge = (status, label) => {
    const styles = {
        PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
        PENDING_APPROVED: 'bg-amber-100 text-amber-800',
        CONFIRMED: 'bg-emerald-100 text-emerald-800',
        CANCELLED: 'bg-slate-100 text-slate-700',
        REJECTED: 'bg-red-100 text-red-700',
        FAILED: 'bg-rose-100 text-rose-700'
    };
    return `<span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${styles[status] || 'bg-slate-100 text-slate-700'}">${label || status || '---'}</span>`;
};

const renderTable = (rows) => {
    if (!rows || rows.length === 0) {
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="6">Nhà hảo tâm chưa có lịch sử quyên góp.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = rows.map((item) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap text-sm font-mono text-slate-700">#${item.donationCode || `DN-${item.donationId}`}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-700">${item.targetLabel || '---'}</td>
            <td class="px-6 py-4 text-sm">
                ${item.targetUrl
                    ? `<a href="${item.targetUrl}" target="_blank" class="font-medium text-primary hover:underline">${item.targetTitle || '---'}</a>`
                    : `<span class="text-slate-700">${item.targetTitle || '---'}</span>`
                }
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-right font-semibold text-slate-900">${formatCurrency(item.amount)}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm">${getStatusBadge(item.status, item.statusLabel)}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-600">${formatDateTime(item.donatedAt)}</td>
        </tr>
    `).join('');
};

const loadHistory = async () => {
    if (!donorId) return;
    try {
        const response = await donorApi.getDonorDonations(donorId, state);
        const pageData = response?.data || {};
        renderTable(pageData.data || []);
        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadHistory();
        });
    } catch (error) {
        console.error('Lỗi tải lịch sử quyên góp:', error);
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-red-500" colspan="6">Không thể tải lịch sử quyên góp.</td></tr>`;
    }
};

document.addEventListener('DOMContentLoaded', loadHistory);
