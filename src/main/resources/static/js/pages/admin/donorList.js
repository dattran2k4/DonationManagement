import { donorApi } from '../../apis/donorApi.js';
import { renderPagination } from '../../components/pagination.js';
import { bindExcelActions } from '../../utils/excelTransfer.js';
import { formatVnd } from '../../utils/currency.js';
import { bindSortButtons, debounce, readStateFromUrl, syncStateToUrl } from '../../utils/adminTable.js';

const DEFAULT_STATE = {
    page: 1,
    size: 50,
    search: '',
    type: '',
    sortBy: 'createdAt',
    sortDir: 'desc'
};

const state = readStateFromUrl(DEFAULT_STATE);

const elements = {
    tableBody: document.getElementById('donorTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('donorSearchInput'),
    typeFilter: document.getElementById('donorTypeFilter'),
    sortButtons: document.querySelectorAll('[data-donor-sort]'),
    exportBtn: document.getElementById('donorExportBtn'),
    templateBtn: document.getElementById('donorTemplateBtn'),
    importBtn: document.getElementById('donorImportBtn'),
    importInput: document.getElementById('donorImportInput')
};

let latestRequestId = 0;
let sortController;

const getDefaultSortDirection = (field) => {
    if (['createdAt', 'numberOfDonations', 'totalDonationAmount'].includes(field)) {
        return 'desc';
    }

    return 'asc';
};

const syncFilterControls = () => {
    if (elements.searchInput) {
        elements.searchInput.value = state.search;
    }

    if (elements.typeFilter) {
        elements.typeFilter.value = state.type;
    }
};

const getInitials = (name) => {
    return name ? name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) : 'NA';
};

const formatCurrency = (amount) => {
    return formatVnd(amount);
};

const formatDonorCode = (id) => {
    if (!id && id !== 0) return '---';
    return `DON-${String(id).padStart(8, '0')}`;
};

const getTypeBadge = (type) => {
    const isOrg = type === 'ORGANIZATION';
    const config = isOrg
        ? { text: 'Tổ chức', class: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300' }
        : { text: 'Cá nhân', class: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300' };

    return `<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.class}">${config.text}</span>`;
};

const renderDonorRow = (donor) => {
    const isOrg = donor.type === 'ORGANIZATION';
    const orgInfo = donor.organization;
    const joinDate = donor.createdAt ? new Date(donor.createdAt).toLocaleDateString('vi-VN') : '---';

    const avatarHtml = isOrg
        ? `<div class="h-10 w-10 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
             <span class="material-symbols-outlined text-[20px]">apartment</span>
           </div>`
        : `<div class="h-10 w-10 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold text-sm">
             ${getInitials(donor.fullName)}
           </div>`;

    return `
    <tr class="hover:bg-slate-50 dark:hover:bg-white/5 transition-colors group">
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex items-center">
                <div class="h-10 w-10 shrink-0">${avatarHtml}</div>
                    <div class="ml-4">
                        ${isOrg && orgInfo ? `
                            <a href="/admin/donors/${donor.id}" class="text-sm font-semibold text-text-main dark:text-white hover:text-primary dark:hover:text-primary transition-colors">
                                ${orgInfo.name}
                            </a>
                            <div class="text-xs text-slate-500 mt-0.5">Mã: ${formatDonorCode(donor.id)}</div>
                            <div class="text-xs text-text-secondary mt-0.5 flex items-center">
                                <span class="material-symbols-outlined text-[12px] mr-1">person_pin</span>
                                Đại diện: ${orgInfo.representative || '---'}
                            </div>
                    ` : `
                        <a href="/admin/donors/${donor.id}" class="text-sm font-semibold text-text-main dark:text-white hover:text-primary dark:hover:text-primary transition-colors">${donor.fullName}</a>
                        <div class="text-xs text-slate-500 mt-0.5">Mã: ${formatDonorCode(donor.id)}</div>
                    `}
                    </div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            ${getTypeBadge(donor.type)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex flex-col gap-1">
                <div class="flex items-center text-sm text-text-main dark:text-slate-200">
                    <span class="material-symbols-outlined text-[16px] mr-1.5 text-slate-400">call</span>
                    ${donor.phone || '---'}
                </div>
                <div class="flex items-center text-sm text-text-secondary">
                    <span class="material-symbols-outlined text-[16px] mr-1.5 text-slate-400">mail</span>
                    ${donor.email || '---'}
                </div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
            ${joinDate}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-text-main dark:text-slate-200 text-center">
            ${donor.numberOfDonations || 0}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-right font-bold text-text-main dark:text-white">
            ${formatCurrency(donor.totalDonationAmount)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-right sticky right-0 bg-white dark:bg-slate-900">
            <a href="/admin/donors/${donor.id}" class="inline-flex items-center rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-primary hover:text-primary">
                Chi tiết
            </a>
        </td>
    </tr>`;
};

const loadDonors = async () => {
    const requestId = ++latestRequestId;

    try {
        syncStateToUrl(state, DEFAULT_STATE);
        const response = await donorApi.getAllDonors(state);
        if (requestId !== latestRequestId) return;

        const pageData = response.data;
        const donors = pageData.data || [];

        if (donors.length === 0) {
            elements.tableBody.innerHTML = `<tr><td colspan="7" class="px-6 py-10 text-center text-text-secondary">Không tìm thấy nhà hảo tâm nào</td></tr>`;
        } else {
            elements.tableBody.innerHTML = donors.map(d => renderDonorRow(d)).join('');
        }

        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            sortController?.updateIndicators();
            loadDonors();
        });
    } catch (error) {
        if (requestId !== latestRequestId) return;
        console.error('Error loading donors:', error);
    }
};

const bindFilters = () => {
    if (elements.searchInput) {
        elements.searchInput.addEventListener('input', debounce((event) => {
            state.search = event.target.value.trim();
            state.page = 1;
            loadDonors();
        }));
    }

    if (elements.typeFilter) {
        elements.typeFilter.addEventListener('change', (event) => {
            state.type = event.target.value;
            state.page = 1;
            loadDonors();
        });
    }

    sortController = bindSortButtons({
        state,
        buttons: elements.sortButtons,
        datasetKey: 'donorSort',
        getDefaultDirection: getDefaultSortDirection,
        onChange: () => loadDonors()
    });
};

document.addEventListener('DOMContentLoaded', () => {
    syncFilterControls();
    bindFilters();
    sortController?.updateIndicators();
    bindExcelActions({
        exportButton: elements.exportBtn,
        templateButton: elements.templateBtn,
        importButton: elements.importBtn,
        importInput: elements.importInput,
        exportUrl: '/api/admin/excel/donors/export',
        templateUrl: '/api/admin/excel/donors/template',
        importUrl: '/api/admin/excel/donors/import',
        getExportParams: () => ({
            search: state.search,
            type: state.type,
            sortBy: state.sortBy,
            sortDir: state.sortDir
        }),
        fallbackFilename: 'nha-hao-tam.xlsx',
        templateFallbackFilename: 'mau-import-nha-hao-tam.xlsx',
        successExportMessage: 'Xuất Excel nhà hảo tâm thành công.',
        successTemplateMessage: 'Đã bắt đầu tải file mẫu nhà hảo tâm.',
        moduleLabel: 'nhà hảo tâm',
        onImportSuccess: () => {
            state.page = 1;
            loadDonors();
        }
    });
    loadDonors();
});

window.viewDonorProfile = (id) => {
    window.location.href = `/admin/donors/${id}`;
};
