import { activityApi } from '../../apis/activityApi.js';
import { renderPagination } from '../../components/pagination.js';
import { bindExcelActions } from '../../utils/excelTransfer.js';
import { formatVnd } from '../../utils/currency.js';
import { bindSortButtons, debounce, readStateFromUrl, syncStateToUrl } from '../../utils/adminTable.js';

const DEFAULT_STATE = {
    page: 1,
    size: 50,
    search: '',
    status: '',
    sortBy: 'startDate',
    sortDir: 'desc'
};

const state = readStateFromUrl(DEFAULT_STATE);
let latestRequestId = 0;
let sortController;

const elements = {
    tableBody: document.getElementById('activityTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('activitySearchInput'),
    statusFilter: document.getElementById('activityStatusFilter'),
    resetFilterBtn: document.getElementById('activityResetFilterBtn'),
    sortButtons: document.querySelectorAll('[data-activity-sort]'),
    exportBtn: document.getElementById('activityExportBtn'),
    templateBtn: document.getElementById('activityTemplateBtn'),
    importBtn: document.getElementById('activityImportBtn'),
    importInput: document.getElementById('activityImportInput')
};

const formatCurrency = (amount) => {
    return formatVnd(amount);
};

const formatActivityCode = (id) => {
    if (!id && id !== 0) return '---';
    return `ACT-${String(id).padStart(8, '0')}`;
};

const formatDateRange = (start, end) => {
    if (!start) return '---';
    const s = new Date(start);
    const startStr = `${s.getDate().toString().padStart(2, '0')}/${(s.getMonth() + 1).toString().padStart(2, '0')}`;

    if (!end) return startStr;
    const e = new Date(end);
    const endStr = `${e.getDate().toString().padStart(2, '0')}/${(e.getMonth() + 1).toString().padStart(2, '0')}`;

    return `${startStr} - ${endStr}`;
};

const getColumnCount = () => 8;

const setTableMessage = (message) => {
    elements.tableBody.innerHTML = `
        <tr>
            <td colspan="${getColumnCount()}" class="p-10 text-center text-slate-500">${message}</td>
        </tr>
    `;
};

const syncFilterControls = () => {
    if (elements.searchInput) elements.searchInput.value = state.search;
    if (elements.statusFilter) elements.statusFilter.value = state.status;
};

const getDefaultSortDirection = (field) => {
    if (['code', 'startDate', 'currentAmount'].includes(field)) {
        return 'desc';
    }

    if (field === 'status') {
        return 'asc';
    }

    return 'asc';
};

const getStatusBadge = (status) => {
    const config = {
        'DRAFT': { text: 'Bản nháp', color: 'slate', dot: 'bg-slate-400' },
        'UPCOMING': { text: 'Sắp diễn ra', color: 'amber', dot: 'bg-amber-500' },
        'ONGOING': { text: 'Đang diễn ra', color: 'emerald', dot: 'bg-primary animate-pulse' },
        'COMPLETED': { text: 'Hoàn thành', color: 'slate', dot: 'bg-slate-400' },
        'CANCELLED': { text: 'Đã hủy', color: 'red', dot: 'bg-red-500' }
    };

    const s = config[status] || config.UPCOMING;

    return `
        <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-${s.color}-100 text-${s.color}-800 dark:bg-${s.color}-900/30 dark:text-${s.color}-300 border border-${s.color}-200 dark:border-${s.color}-800">
            <span class="w-1.5 h-1.5 rounded-full ${s.dot}"></span>
            ${s.text}
        </span>
    `;
};

const renderActivityRow = (activity) => {
    const target = activity.targetAmount || 0;
    const current = activity.currentAmount || 0;
    const percent = target > 0 ? Math.round((current / target) * 100) : 0;
    const progressWidth = Math.min(percent, 100);
    const year = activity.startDate ? new Date(activity.startDate).getFullYear() : '---';

    return `
    <tr class="hover:bg-background-light dark:hover:bg-gray-800/50 transition-colors group">
        <td class="px-6 py-4 whitespace-nowrap font-mono text-sm text-slate-700 dark:text-slate-300">${formatActivityCode(activity.id)}</td>
        <td class="px-6 py-4 whitespace-nowrap">
            <a href="/admin/activities/${activity.id}" class="text-sm font-medium text-text-main dark:text-white hover:text-primary transition-colors">${activity.name}</a>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="text-sm text-text-main dark:text-gray-300">${activity.event?.name || 'Không thuộc sự kiện'}</div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex flex-col">
                <span class="text-sm text-text-main dark:text-gray-300 font-medium">
                    ${formatDateRange(activity.startDate, activity.endDate)}
                </span>
                <span class="text-xs text-text-secondary">Năm ${year}</span>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex items-center text-sm text-text-secondary">
                <span class="material-symbols-outlined text-[16px] mr-1">location_on</span>
                ${activity.location || 'Chưa xác định'}
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap align-middle">
            <div class="w-full">
                <div class="flex items-center justify-between mb-1.5 gap-2">
                    <span class="text-xs font-semibold text-text-main dark:text-white">${formatCurrency(current)}</span>
                    <span class="text-xs text-text-secondary">/ ${formatCurrency(target)}</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-2 dark:bg-gray-700 overflow-hidden">
                    <div class="bg-primary h-2 rounded-full transition-all duration-500"
                         style="width: ${progressWidth}%"></div>
                </div>
                <div class="mt-1 text-right text-[10px] text-primary-dark font-medium">Đạt ${percent}%</div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            ${getStatusBadge(activity.status)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-right">
            <a href="/admin/activities/${activity.id}" class="inline-flex items-center rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-primary hover:text-primary">
                Chi tiết
            </a>
        </td>
    </tr>`;
};

const loadActivities = async () => {
    const requestId = ++latestRequestId;

    try {
        setTableMessage('Đang tải dữ liệu...');
        elements.paginationContainer.innerHTML = '';
        syncStateToUrl(state, DEFAULT_STATE);

        const response = await activityApi.getAllActivities(state);
        if (requestId !== latestRequestId) return;

        const pageData = response.data;
        const activities = pageData.data || [];

        if (activities.length === 0) {
            setTableMessage('Không có hoạt động nào');
            elements.paginationContainer.innerHTML = '';
            return;
        }

        elements.tableBody.innerHTML = activities.map((activity) => renderActivityRow(activity)).join('');

        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            sortController?.updateIndicators();
            loadActivities();
        });
    } catch (error) {
        if (requestId !== latestRequestId) return;
        setTableMessage('Không thể tải danh sách hoạt động');
        elements.paginationContainer.innerHTML = '';
        console.error('Lỗi khi tải Activities:', error);
    }
};

const bindFilters = () => {
    if (elements.searchInput) {
        elements.searchInput.addEventListener('input', debounce((event) => {
            state.search = event.target.value.trim();
            state.page = 1;
            loadActivities();
        }));
    }

    if (elements.statusFilter) {
        elements.statusFilter.addEventListener('change', (event) => {
            state.status = event.target.value;
            state.page = 1;
            loadActivities();
        });
    }

    if (elements.resetFilterBtn) {
        elements.resetFilterBtn.addEventListener('click', () => {
            state.search = '';
            state.status = '';
            state.page = 1;

            if (elements.searchInput) elements.searchInput.value = '';
            if (elements.statusFilter) elements.statusFilter.value = '';

            loadActivities();
        });
    }

    sortController = bindSortButtons({
        state,
        buttons: elements.sortButtons,
        datasetKey: 'activitySort',
        getDefaultDirection: getDefaultSortDirection,
        onChange: () => loadActivities()
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
        exportUrl: '/api/admin/excel/activities/export',
        templateUrl: '/api/admin/excel/activities/template',
        importUrl: '/api/admin/excel/activities/import',
        getExportParams: () => ({
            search: state.search,
            status: state.status,
            sortBy: state.sortBy,
            sortDir: state.sortDir
        }),
        fallbackFilename: 'hoat-dong.xlsx',
        templateFallbackFilename: 'mau-import-hoat-dong.xlsx',
        successExportMessage: 'Xuất Excel hoạt động thành công.',
        successTemplateMessage: 'Đã bắt đầu tải file mẫu hoạt động.',
        moduleLabel: 'hoạt động',
        onImportSuccess: () => {
            state.page = 1;
            loadActivities();
        }
    });
    loadActivities();
});

window.addEventListener('pageshow', (event) => {
    const navigationEntry = performance.getEntriesByType('navigation')[0];
    const isBackForwardNavigation = event.persisted || navigationEntry?.type === 'back_forward';

    if (!isBackForwardNavigation) return;

    syncFilterControls();
    sortController?.updateIndicators();
    loadActivities();
});
