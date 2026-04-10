import {eventApi} from '../../apis/eventApi.js';
import {renderPagination} from '../../components/pagination.js';
import {bindExcelActions} from '../../utils/excelTransfer.js';
import {formatVnd} from '../../utils/currency.js';

const state = {
    page: 1,
    size: 50,
    search: '',
    status: '',
    categoryId: '',
    sortBy: 'id',
    sortDir: 'desc'
};

const elements = {
    tableBody: document.getElementById('eventTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('searchFilter'),
    statusSelect: document.getElementById('statusFilter'),
    categorySelect: document.getElementById('categoryFilter'),
    sortSelect: document.getElementById('sortFilter'),
    exportBtn: document.getElementById('eventExportBtn'),
    importBtn: document.getElementById('eventImportBtn'),
    importInput: document.getElementById('eventImportInput')
};

let searchDebounceId = null;

const formatEventCode = (id) => {
    if (!id && id !== 0) return '---';
    return `EVT-${String(id).padStart(8, '0')}`;
};

const formatMoney = (amount) => {
    return formatVnd(amount);
};

// Hàm tiện ích lấy style cho Badge Trạng thái
const getStatusBadge = (status) => {
    const styles = {
        'ONGOING': {
            text: 'Đang diễn ra',
            colorClass: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
            dotClass: 'bg-green-500'
        },
        'DRAFT': {
            text: 'Bản nháp',
            colorClass: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
            dotClass: 'bg-slate-400'
        },
        'COMPLETED': {
            text: 'Hoàn thành',
            colorClass: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
            dotClass: 'bg-slate-400'
        },
        'UPCOMING': {
            text: 'Sắp diễn ra',
            colorClass: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400',
            dotClass: 'bg-amber-500'
        }
    };
    const s = styles[status] || styles['UPCOMING'];
    return `
        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${s.colorClass}">
            <span class="w-1.5 h-1.5 rounded-full ${s.dotClass}"></span> ${s.text}
        </span>
    `;
};

// Hàm Render Bảng
const renderTable = (data) => {
    const colspan = 6;
    if (!data || data.length === 0) {
        elements.tableBody.innerHTML = `<tr><td colspan="${colspan}" class="px-6 py-8 text-center text-slate-500">Không tìm thấy sự kiện nào.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = data.map(item => {
        // Tính phần trăm gây quỹ
        const percent = item.targetAmount > 0 ? Math.min(Math.round((item.currentAmount / item.targetAmount) * 100), 100) : 0;
        const isCompleted = percent >= 100;
        return `
        <tr class="group hover:bg-slate-50 dark:hover:bg-white/5 transition-colors">
            <td class="px-6 py-4 font-mono text-sm text-slate-700 dark:text-slate-300">${formatEventCode(item.id)}</td>
            <td class="px-6 py-4">
                <div class="h-10 w-10 rounded-lg bg-cover bg-center shadow-sm" 
                     style="background-image: url('${item.thumbnailUrl || '/images/default-event.png'}')"></div>
            </td>
            <td class="px-6 py-4">
                <a href="/admin/events/${item.id}" class="font-semibold text-slate-900 dark:text-slate-100 hover:text-primary dark:hover:text-primary transition-colors">${item.name}</a>
                <div class="text-xs text-slate-500 mt-0.5">Mã: ${formatEventCode(item.id)}</div>
            </td>
            <td class="px-6 py-4">
                ${getStatusBadge(item.status)}
            </td>
            <td class="px-6 py-4">
                <div class="flex justify-between mb-1.5 text-xs">
                    <span class="font-medium text-slate-700 dark:text-slate-300">${formatMoney(item.currentAmount)}</span>
                    <span class="text-slate-500">Mục tiêu: ${formatMoney(item.targetAmount)}</span>
                </div>
                <div class="w-full bg-slate-100 dark:bg-slate-700 rounded-full h-2 overflow-hidden">
                    <div class="bg-primary h-2 rounded-full" style="width: ${percent}%"></div>
                </div>
                <div class="mt-1 text-right text-[10px] ${isCompleted ? 'text-green-600 font-bold' : 'text-slate-400'}">
                    ${isCompleted ? 'Hoàn thành' : percent + '%'}
                </div>
            </td>
            <td class="px-6 py-4 text-slate-600 dark:text-slate-300">
                ${item.startDate} - ${item.endDate}
            </td>
        </tr>
        `;
    }).join('');
};

const loadEvents = async () => {
    try {
        const response = await eventApi.getEvents(buildEventQueryParams());
        const data = response.data;
        renderTable(data.data);

        renderPagination(data, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadEvents();
        });
    } catch (error) {
        console.error("Lỗi tải danh sách sự kiện:", error);
        elements.tableBody.innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-red-500">Không thể tải dữ liệu sự kiện.</td></tr>`;
    }
};

function buildEventQueryParams() {
    return {
        page: state.page,
        size: state.size,
        search: state.search,
        status: state.status,
        categoryIds: state.categoryId,
        sortBy: state.sortBy,
        sortDir: state.sortDir
    };
}

function bindFilters() {
    if (elements.searchInput) {
        elements.searchInput.addEventListener('input', (e) => {
            clearTimeout(searchDebounceId);
            searchDebounceId = setTimeout(() => {
                state.search = e.target.value.trim();
                state.page = 1;
                loadEvents();
            }, 300);
        });
    }

    if (elements.statusSelect) {
        elements.statusSelect.addEventListener('change', (e) => {
            state.status = e.target.value;
            state.page = 1;
            loadEvents();
        });
    }

    if (elements.categorySelect) {
        elements.categorySelect.addEventListener('change', (e) => {
            state.categoryId = e.target.value;
            state.page = 1;
            loadEvents();
        });
    }

    if (elements.sortSelect) {
        elements.sortSelect.addEventListener('change', (e) => {
            const [sortBy, sortDir] = e.target.value.split(':');
            state.sortBy = sortBy || 'id';
            state.sortDir = sortDir || 'desc';
            state.page = 1;
            loadEvents();
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    bindFilters();
    bindExcelActions({
        exportButton: elements.exportBtn,
        importButton: elements.importBtn,
        importInput: elements.importInput,
        exportUrl: '/api/admin/excel/events/export',
        importUrl: '/api/admin/excel/events/import',
        getExportParams: buildEventQueryParams,
        fallbackFilename: 'su-kien.xlsx',
        successExportMessage: 'Xuất Excel sự kiện thành công.',
        onImportSuccess: () => {
            state.page = 1;
            loadEvents();
        }
    });
    loadEvents();
});
