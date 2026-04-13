import { transactionApi } from '../../apis/transactionApi.js';
import { renderPagination } from '../../components/pagination.js';
import { bindExcelActions } from '../../utils/excelTransfer.js';
import { formatVnd } from '../../utils/currency.js';
import { bindSortButtons, debounce, readStateFromUrl, syncStateToUrl } from '../../utils/adminTable.js';

const DEFAULT_STATE = {
    page: 1,
    size: 50,
    search: '',
    method: '',
    sortBy: 'transactionDateTime',
    sortDir: 'desc'
};

const state = readStateFromUrl(DEFAULT_STATE);

let sortController;

const elements = {
    tableBody: document.getElementById('transactionTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('transactionSearchInput'),
    methodFilter: document.getElementById('transactionMethodFilter'),
    resetFilterBtn: document.getElementById('transactionResetFilterBtn'),
    sortButtons: document.querySelectorAll('[data-transaction-sort]'),
    exportBtn: document.getElementById('transactionExportBtn'),
    templateBtn: document.getElementById('transactionTemplateBtn'),
    importBtn: document.getElementById('transactionImportBtn'),
    importInput: document.getElementById('transactionImportInput')
};

const syncFilterControls = () => {
    if (elements.searchInput) elements.searchInput.value = state.search;
    if (elements.methodFilter) elements.methodFilter.value = state.method;
};

const getDefaultSortDirection = (field) => {
    if (['code', 'transactionCode', 'amount', 'transactionDateTime'].includes(field)) {
        return 'desc';
    }

    return 'asc';
};

// 1. Format tiền tệ
const formatCurrency = (amount) => {
    return formatVnd(amount);
};

// 2. Format Thời gian
const formatDateTime = (dateStr) => {
    if (!dateStr) return { date: '---', time: '' };
    const date = new Date(dateStr);
    return {
        date: date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }),
        time: date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    };
};

// 3. Render Row Giao dịch
const renderTransactionRow = (txn) => {
    const dt = formatDateTime(txn.transactionDateTime || txn.createdAt);
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
        syncStateToUrl(state, DEFAULT_STATE);
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
            sortController?.updateIndicators();
            loadTransactions();
        });

    } catch (error) {
        console.error("Lỗi khi tải giao dịch:", error);
    }
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

    sortController = bindSortButtons({
        state,
        buttons: elements.sortButtons,
        datasetKey: 'transactionSort',
        getDefaultDirection: getDefaultSortDirection,
        onChange: () => loadTransactions()
    });
};

// Khởi chạy khi load trang
document.addEventListener('DOMContentLoaded', () => {
    syncFilterControls();
    bindFilters();
    sortController?.updateIndicators();
    bindExcelActions({
        exportButton: elements.exportBtn,
        templateButton: elements.templateBtn,
        importButton: elements.importBtn,
        importInput: elements.importInput,
        exportUrl: '/api/admin/excel/transactions/export',
        templateUrl: '/api/admin/excel/transactions/template',
        importUrl: '/api/admin/excel/transactions/import',
        getExportParams: () => ({
            search: state.search,
            method: state.method,
            sortBy: state.sortBy,
            sortDir: state.sortDir
        }),
        fallbackFilename: 'giao-dich.xlsx',
        templateFallbackFilename: 'mau-import-giao-dich.xlsx',
        successExportMessage: 'Xuất Excel giao dịch thành công.',
        successTemplateMessage: 'Đã bắt đầu tải file mẫu giao dịch.',
        moduleLabel: 'giao dịch',
        onImportSuccess: () => {
            state.page = 1;
            loadTransactions();
        }
    });
    loadTransactions();
});
