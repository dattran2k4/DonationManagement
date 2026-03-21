import {transactionApi} from '../../apis/transactionApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {page: 1, size: 10, search: '', method: ''};

const elements = {
    tableBody: document.getElementById('transactionTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('transactionSearchInput'),
    methodFilter: document.getElementById('transactionMethodFilter'),
    resetFilterBtn: document.getElementById('transactionResetFilterBtn')
};

// 1. Format tiền tệ
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' ₫';
};

// 2. Format Thời gian
const formatDateTime = (dateStr) => {
    if (!dateStr) return {date: '---', time: ''};
    const date = new Date(dateStr);
    return {
        date: date.toLocaleDateString('vi-VN', {day: '2-digit', month: '2-digit', year: 'numeric'}),
        time: date.toLocaleTimeString('vi-VN', {hour: '2-digit', minute: '2-digit'})
    };
};

// 3. Render Row Giao dịch
const renderTransactionRow = (txn) => {
    const dt = formatDateTime(txn.createdAt);
    const isUnlinked = !txn.donationCode;

    // CSS Class cho hàng Unlinked
    const rowClass = isUnlinked
        ? 'bg-amber-50/50 dark:bg-amber-900/10 hover:bg-amber-50 dark:hover:bg-amber-900/20 border-l-4 border-l-amber-500'
        : 'hover:bg-slate-50 dark:hover:bg-slate-800/50';

    return `
    <tr class="${rowClass} transition-colors group">
        <td class="p-4 text-sm font-medium text-blue-400 dark:text-white whitespace-nowrap font-mono">
            <a class="text-blue-400" href="/admin/transactions/${txn.id}">${txn.transactionCode || `TXN-${txn.id}`}</a>
        </td>
        <td class="p-4 text-sm font-bold text-slate-900 dark:text-white text-right whitespace-nowrap">
            ${formatCurrency(txn.amount)}
        </td>
        <td class="p-4 text-sm text-slate-600 dark:text-slate-300">
            <div class="flex flex-col">
                <span class="font-medium text-slate-900 dark:text-white">${txn.counterAccountName || 'Không xác định'}</span>
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
            ${txn.paymentMethodValue || txn.paymentMethod || '---'}
        </td>
        <td class="p-4 text-sm text-slate-600 dark:text-slate-300 whitespace-nowrap">
            <div class="flex flex-col">
                <span>${dt.date}</span>
                <span class="text-xs text-slate-400">${dt.time}</span>
            </div>
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

const debounce = (fn, delay = 350) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
};

const bindFilters = () => {
    if (elements.searchInput) {
        elements.searchInput.addEventListener('input', debounce((event) => {
            state.search = event.target.value.trim();
            state.page = 1;
            loadTransactions();
        }));
    }

    if (elements.methodFilter) {
        elements.methodFilter.addEventListener('change', (event) => {
            state.method = event.target.value;
            state.page = 1;
            loadTransactions();
        });
    }

    if (elements.resetFilterBtn) {
        elements.resetFilterBtn.addEventListener('click', () => {
            state.search = '';
            state.method = '';
            state.page = 1;

            if (elements.searchInput) elements.searchInput.value = '';
            if (elements.methodFilter) elements.methodFilter.value = '';

            loadTransactions();
        });
    }
};

// Khởi chạy khi load trang
document.addEventListener('DOMContentLoaded', () => {
    bindFilters();
    loadTransactions();
});
