import {donationApi} from '../../apis/donationApi.js';
import {renderPagination} from '../../components/pagination.js';
import {bindExcelActions} from '../../utils/excelTransfer.js';
import {formatDonationCode, getDonationStatusUi, DONATION_PAYMENT_METHOD_LABELS} from '../../utils/donationUi.js';
import {formatVnd} from '../../utils/currency.js';

const state = {
    page: 1,
    size: 50,
    search: '',
    status: '',
    target: '',
    paymentMethod: '',
    minAmount: '',
    maxAmount: ''
};

const elements = {
    tableBody: document.getElementById('donationTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('donationSearchInput'),
    statusFilter: document.getElementById('donationStatusFilter'),
    targetFilter: document.getElementById('donationTargetFilter'),
    paymentMethodFilter: document.getElementById('donationPaymentMethodFilter'),
    amountRangeToggle: document.getElementById('donationAmountRangeToggle'),
    amountRangePanel: document.getElementById('donationAmountRangePanel'),
    minAmountFilter: document.getElementById('donationMinAmountFilter'),
    maxAmountFilter: document.getElementById('donationMaxAmountFilter'),
    applyAmountFilterBtn: document.getElementById('donationApplyAmountFilterBtn'),
    clearAmountFilterBtn: document.getElementById('donationClearAmountFilterBtn'),
    resetFilterBtn: document.getElementById('donationResetFilterBtn'),
    exportBtn: document.getElementById('donationExportBtn'),
    importBtn: document.getElementById('donationImportBtn'),
    importInput: document.getElementById('donationImportInput')
};

const updateAmountRangeButtonState = () => {
    if (!elements.amountRangeToggle) return;

    const hasAmountFilter = state.minAmount !== '' || state.maxAmount !== '';
    elements.amountRangeToggle.classList.toggle('border-primary', hasAmountFilter);
    elements.amountRangeToggle.classList.toggle('text-primary', hasAmountFilter);
    elements.amountRangeToggle.classList.toggle('bg-primary/5', hasAmountFilter);
};

const toggleAmountRangePanel = () => {
    if (!elements.amountRangePanel) return;
    elements.amountRangePanel.classList.toggle('hidden');
};

const applyAmountFilter = () => {
    const minAmountValue = elements.minAmountFilter?.value?.trim() || '';
    const maxAmountValue = elements.maxAmountFilter?.value?.trim() || '';

    if (minAmountValue && Number(minAmountValue) < 0) {
        alert('Số tiền tối thiểu phải lớn hơn hoặc bằng 0.');
        return;
    }

    if (maxAmountValue && Number(maxAmountValue) < 0) {
        alert('Số tiền tối đa phải lớn hơn hoặc bằng 0.');
        return;
    }

    if (minAmountValue && maxAmountValue && Number(minAmountValue) > Number(maxAmountValue)) {
        alert('Khoảng số tiền không hợp lệ. Vui lòng nhập "từ" nhỏ hơn hoặc bằng "đến".');
        return;
    }

    state.minAmount = minAmountValue;
    state.maxAmount = maxAmountValue;
    state.page = 1;
    updateAmountRangeButtonState();
    loadDonations();
};

const clearAmountFilter = () => {
    state.minAmount = '';
    state.maxAmount = '';
    if (elements.minAmountFilter) elements.minAmountFilter.value = '';
    if (elements.maxAmountFilter) elements.maxAmountFilter.value = '';
    state.page = 1;
    updateAmountRangeButtonState();
    loadDonations();
};

const formatCurrency = (amount) => {
    return formatVnd(amount);
};

const getStatusBadge = (status) => {
    const base = getDonationStatusUi(status);
    const styles = {
        PENDING_APPROVED: {
            text: base.text,
            class: base.className,
            dot: '<span class="h-1.5 w-1.5 rounded-full bg-amber-500 animate-pulse"></span>',
            rowClass: 'bg-amber-50/50 dark:bg-amber-900/10'
        },
        PENDING_PAYMENT: {
            text: base.text,
            class: base.className,
            dot: '<span class="h-1.5 w-1.5 rounded-full bg-yellow-500"></span>',
            rowClass: ''
        },
        CONFIRMED: {
            text: base.text,
            class: base.className,
            dot: '',
            rowClass: ''
        },
        REJECTED: {
            text: base.text,
            class: base.className,
            dot: '',
            rowClass: 'opacity-75'
        },
        FAILED: {
            text: base.text,
            class: base.className,
            dot: '',
            rowClass: 'opacity-75'
        }
    };
    return styles[status] || styles.PENDING_APPROVED;
};

const getPaymentMethodIcon = (method) => {
    const icons = {
        CASH: {icon: 'payments', label: DONATION_PAYMENT_METHOD_LABELS.CASH},
        BANK_TRANSFER_ONLINE: {icon: 'account_balance', label: DONATION_PAYMENT_METHOD_LABELS.BANK_TRANSFER_ONLINE},
        BANK_TRANSFER_OFFLINE: {icon: 'receipt_long', label: DONATION_PAYMENT_METHOD_LABELS.BANK_TRANSFER_OFFLINE}
    };
    return icons[method] || {icon: 'help_outline', label: method};
};

const getTargetLabel = (target) => {
    const labels = {
        EVENT: 'Sự kiện',
        ACTIVITY: 'Hoạt động',
        NONE: 'Không gắn mục tiêu'
    };
    return labels[target] || '---';
};

const getViaLabel = (donationVia) => {
    return donationVia === 'STAFF' ? 'Nội bộ' : 'Website';
};

const renderTable = (donations) => {
    if (!donations || donations.length === 0) {
        elements.tableBody.innerHTML = '<tr><td colspan="6" class="px-6 py-10 text-center text-slate-500">Chưa có dữ liệu quyên góp nào.</td></tr>';
        return;
    }

    elements.tableBody.innerHTML = donations.map((item) => {
        const statusStyle = getStatusBadge(item.status);
        const payment = getPaymentMethodIcon(item.paymentMethod);
        const donatedAt = item.donatedAt
            ? new Date(item.donatedAt).toLocaleString('vi-VN', {
                hour: '2-digit',
                minute: '2-digit',
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            })
            : '---';

        return `
        <tr class="${statusStyle.rowClass} hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap">
                <a href="/admin/donations/${item.id}" class="text-sm font-mono text-slate-900 dark:text-white font-medium hover:text-primary dark:hover:text-primary transition-colors">${formatDonationCode(item.id)}</a>
                <div class="text-xs text-slate-500 mt-0.5">${donatedAt}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm font-medium text-slate-900 dark:text-white">${item.donorName || 'Ẩn danh'}</div>
                <div class="text-xs text-slate-500">${getViaLabel(item.donationVia)}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right">
                <div class="text-sm font-bold ${item.status === 'REJECTED' ? 'text-slate-500 line-through' : 'text-slate-900 dark:text-white'}">
                    ${formatCurrency(item.amount)}
                </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-slate-700 dark:text-slate-300">${item.objectName || '---'}</div>
                <div class="text-xs text-slate-500">${getTargetLabel(item.target)}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap hidden xl:table-cell">
                <div class="flex items-center gap-1.5">
                    <span class="material-symbols-outlined text-[16px] text-slate-400">${payment.icon}</span>
                    <span class="text-sm text-slate-600 dark:text-slate-400">${payment.label}</span>
                </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="${statusStyle.class} gap-1.5">
                    ${statusStyle.dot}
                    ${statusStyle.text}
                </span>
            </td>
        </tr>
        `;
    }).join('');
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
            loadDonations();
        }));
    }

    if (elements.statusFilter) {
        elements.statusFilter.addEventListener('change', (event) => {
            state.status = event.target.value;
            state.page = 1;
            loadDonations();
        });
    }

    if (elements.targetFilter) {
        elements.targetFilter.addEventListener('change', (event) => {
            state.target = event.target.value;
            state.page = 1;
            loadDonations();
        });
    }

    if (elements.paymentMethodFilter) {
        elements.paymentMethodFilter.addEventListener('change', (event) => {
            state.paymentMethod = event.target.value;
            state.page = 1;
            loadDonations();
        });
    }

    if (elements.amountRangeToggle) {
        elements.amountRangeToggle.addEventListener('click', toggleAmountRangePanel);
    }

    if (elements.applyAmountFilterBtn) {
        elements.applyAmountFilterBtn.addEventListener('click', applyAmountFilter);
    }

    if (elements.clearAmountFilterBtn) {
        elements.clearAmountFilterBtn.addEventListener('click', clearAmountFilter);
    }

    if (elements.minAmountFilter) {
        elements.minAmountFilter.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                applyAmountFilter();
            }
        });
    }

    if (elements.maxAmountFilter) {
        elements.maxAmountFilter.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                applyAmountFilter();
            }
        });
    }

    if (elements.resetFilterBtn) {
        elements.resetFilterBtn.addEventListener('click', () => {
            state.search = '';
            state.status = '';
            state.target = '';
            state.paymentMethod = '';
            state.minAmount = '';
            state.maxAmount = '';
            state.page = 1;

            if (elements.searchInput) elements.searchInput.value = '';
            if (elements.statusFilter) elements.statusFilter.value = '';
            if (elements.targetFilter) elements.targetFilter.value = '';
            if (elements.paymentMethodFilter) elements.paymentMethodFilter.value = '';
            if (elements.minAmountFilter) elements.minAmountFilter.value = '';
            if (elements.maxAmountFilter) elements.maxAmountFilter.value = '';
            updateAmountRangeButtonState();

            loadDonations();
        });
    }
};

const loadDonations = async () => {
    try {
        const response = await donationApi.getDonations(state);
        const data = response.data;
        renderTable(data.data);

        renderPagination(data, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadDonations();
        });
    } catch (error) {
        console.error('Lỗi khi tải danh sách quyên góp:', error);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    updateAmountRangeButtonState();
    bindFilters();
    bindExcelActions({
        exportButton: elements.exportBtn,
        importButton: elements.importBtn,
        importInput: elements.importInput,
        exportUrl: '/api/admin/excel/donations/export',
        importUrl: '/api/admin/excel/donations/import',
        getExportParams: () => ({
            search: state.search,
            status: state.status,
            target: state.target,
            paymentMethod: state.paymentMethod,
            minAmount: state.minAmount,
            maxAmount: state.maxAmount
        }),
        fallbackFilename: 'quyen-gop.xlsx',
        successExportMessage: 'Xuất Excel quyên góp thành công.',
        onImportSuccess: () => {
            state.page = 1;
            loadDonations();
        }
    });
    loadDonations();
});
