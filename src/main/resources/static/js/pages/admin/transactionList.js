import {transactionApi} from '../../apis/transactionApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {page: 1, size: 10, status: '', method: ''};

const elements = {
    tableBody: document.getElementById('transactionTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

// 1. Format tiền tệ
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' ₫';
};

// 2. Format Thời gian
const formatDateTime = (dateStr) => {
    const date = new Date(dateStr);
    return {
        date: date.toLocaleDateString('en-US', {month: 'short', day: '2-digit', year: 'numeric'}),
        time: date.toLocaleTimeString('en-US', {hour: '2-digit', minute: '2-digit', hour12: true})
    };
};

// 3. Render Row Giao dịch
const renderTransactionRow = (txn) => {
    const dt = formatDateTime(txn.createdAt);

    // Logic: Nếu không có donationCode/donationId thì coi là Unlinked (Chưa khớp lệnh)
    const isUnlinked = !txn.donationCode && !txn.donationId;

    // CSS Class cho hàng Unlinked
    const rowClass = isUnlinked
        ? 'bg-amber-50/50 dark:bg-amber-900/10 hover:bg-amber-50 dark:hover:bg-amber-900/20 border-l-4 border-l-amber-500'
        : 'hover:bg-slate-50 dark:hover:bg-[#14241d]';

    return `
    <tr class="${rowClass} transition-colors group">
        <td class="p-4 text-sm font-medium text-blue-400 dark:text-white whitespace-nowrap font-mono">
            <a class="text-blue-400" href="/admin/transactions/${txn.id}">${txn.transactionCode || `TXN-${txn.id}`}</a>
        </td>
        <td class="p-4 text-sm text-slate-600 dark:text-slate-300 whitespace-nowrap">
            <div class="flex flex-col">
                <span>${dt.date}</span>
                <span class="text-xs text-slate-400">${dt.time}</span>
            </div>
        </td>
        <td class="p-4 text-sm font-bold text-slate-900 dark:text-white text-right whitespace-nowrap">
            ${formatCurrency(txn.amount)}
        </td>
        <td class="p-4 text-sm text-slate-600 dark:text-slate-300">
            <div class="flex flex-col">
                <span class="font-medium text-slate-900 dark:text-white">${txn.counterAccountName || 'Unknown'}</span>
                <span class="text-xs text-slate-400">${txn.counterAccountNumber || ''}</span>
            </div>
        </td>
        <td class="p-4 text-sm text-slate-600 dark:text-slate-300 whitespace-nowrap">
            ${txn.donationCode ? `
                <a class="text-emerald-600 dark:text-emerald-400 hover:underline font-medium" 
                   href="/admin/donations?search=${txn.donationCode}">
                   #${txn.donationCode}
                </a>
            ` : `<span class="text-slate-400 italic">--</span>`}
        </td>
        <td class="p-4 text-sm text-slate-600 dark:text-slate-300 whitespace-nowrap font-medium">
            ${txn.paymentMethod}
        </td>
    </tr>`;
};

// 4. Hàm Load dữ liệu chính
const loadTransactions = async () => {
    try {
        const response = await transactionApi.getAllTransactions(state);

        const pageData = response.data;
        const transactions = pageData.data || [];

        if (transactions.length === 0) {
            elements.tableBody.innerHTML = `
                <tr><td colspan="6" class="p-10 text-center text-slate-500">Không tìm thấy giao dịch nào.</td></tr>
            `;
            return;
        }

        elements.tableBody.innerHTML = transactions.map(txn => renderTransactionRow(txn)).join('');

        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadTransactions();
        });

    } catch (error) {
        console.error("Lỗi khi tải giao dịch:", error);
    }
};

// Khởi chạy khi load trang
document.addEventListener('DOMContentLoaded', loadTransactions);