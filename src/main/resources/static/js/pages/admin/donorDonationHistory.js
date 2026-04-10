import {donorApi} from '../../apis/donorApi.js';
import {renderPagination} from '../../components/pagination.js';
import {getDonationStatusUi, formatDonationCode} from '../../utils/donationUi.js';
import {formatVnd} from '../../utils/currency.js';

const donorId = window.__DONOR_ID__;
const state = {
    page: 1,
    size: 10
};

const elements = {
    tableBody: document.getElementById('donorDonationTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

const formatCurrency = (amount) => formatVnd(amount);

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
    const statusUi = getDonationStatusUi(status);
    return `<span class="${statusUi.className}">${label || statusUi.text || status || '---'}</span>`;
};

const renderTable = (rows) => {
    if (!rows || rows.length === 0) {
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="6">Nhà hảo tâm chưa có lịch sử quyên góp.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = rows.map((item) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap text-sm font-mono text-slate-700">${item.donationCode || formatDonationCode(item.donationId)}</td>
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
