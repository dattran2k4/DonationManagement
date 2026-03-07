import {activityApi} from '../../apis/activityApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {page: 1, size: 2, search: '', status: ''};

const elements = {
    tableBody: document.getElementById('activityTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

// 1. Hàm định dạng tiền tệ (VD: 1.000.000đ)
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + 'đ';
};

// 2. Hàm định dạng ngày tháng (VD: 12/05 - 15/05)
const formatDateRange = (start, end) => {
    if (!start) return '---';
    const s = new Date(start);
    const startStr = `${s.getDate().toString().padStart(2, '0')}/${(s.getMonth() + 1).toString().padStart(2, '0')}`;

    if (!end) return startStr;
    const e = new Date(end);
    const endStr = `${e.getDate().toString().padStart(2, '0')}/${(e.getMonth() + 1).toString().padStart(2, '0')}`;

    return `${startStr} - ${endStr}`;
};

// 3. Hàm xử lý Badge Trạng thái
const getStatusBadge = (status) => {
    const config = {
        'UPCOMING': {text: 'Sắp diễn ra', color: 'amber', dot: 'bg-amber-500'},
        'ONGOING': {text: 'Đang diễn ra', color: 'emerald', dot: 'bg-primary animate-pulse'},
        'COMPLETED': {text: 'Đã kết thúc', color: 'gray', dot: 'bg-gray-500'},
        'CANCELLED': {text: 'Đã hủy', color: 'red', dot: 'bg-red-500'}
    };

    const s = config[status] || config['UPCOMING'];

    return `
        <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-${s.color}-100 text-${s.color}-800 dark:bg-${s.color}-900/30 dark:text-${s.color}-300 border border-${s.color}-200 dark:border-${s.color}-800">
            <span class="w-1.5 h-1.5 rounded-full ${s.dot}"></span>
            ${s.text}
        </span>
    `;
};

// 4. Render Row
const renderActivityRow = (activity) => {
    // Tính phần trăm mục tiêu
    const target = activity.targetAmount || 0;
    const current = activity.currentAmount || 0;
    const percent = target > 0 ? Math.round((current / target) * 100) : 0;
    const progressWidth = Math.min(percent, 100); // Không vượt quá 100% thanh bar

    return `
    <tr class="hover:bg-background-light dark:hover:bg-gray-800/50 transition-colors group">
        <td class="px-6 py-4 whitespace-nowrap">
            <a href="/admin/activities/${activity.id}/form" class="text-sm text-orange-600 font-medium text-text-main dark:text-white">${activity.name}</a>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="text-sm text-text-main dark:text-gray-300">${activity.event?.name || 'Không thuộc sự kiện'}</div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex flex-col">
                <span class="text-sm text-text-main dark:text-gray-300 font-medium">
                    ${formatDateRange(activity.startDate, activity.endDate)}
                </span>
                <span class="text-xs text-text-secondary">Năm ${new Date(activity.startDate).getFullYear()}</span>
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
        <td class="px-6 py-4 whitespace-nowrap text-center text-sm font-medium">
            <div class="flex items-center justify-center gap-3">
                <button onclick="viewActivityDetail(${activity.id})" class="group/btn flex items-center justify-center w-8 h-8 rounded-full text-gray-400 hover:text-primary-dark hover:bg-primary/10 transition-all duration-200" title="Xem chi tiết">
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                <div class="h-4 w-px bg-border-color"></div>
                <button onclick="editActivity(${activity.id})" class="group/btn flex items-center justify-center w-8 h-8 rounded-full text-gray-400 hover:text-blue-600 hover:bg-blue-50 transition-all duration-200" title="Cập nhật">
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                </button>
            </div>
        </td>
    </tr>`;
};

// 5. Hàm tải dữ liệu
const loadActivities = async () => {
    try {
        const response = await activityApi.getAllActivities(state);

        const pageData = response.data;
        console.log(pageData)
        const activities = pageData.data || [];

        if (activities.length === 0) {
            elements.tableBody.innerHTML = `<tr><td colspan="7" class="p-10 text-center text-slate-500">Không có hoạt động nào</td></tr>`;
            return;
        }

        elements.tableBody.innerHTML = activities.map(a => renderActivityRow(a)).join('');

        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadActivities();
        });
    } catch (error) {
        console.error("Lỗi khi tải Activities:", error);
    }
};

// Khởi chạy
document.addEventListener('DOMContentLoaded', loadActivities);

// Các hàm toàn cục
window.viewActivityDetail = (id) => window.location.href = `/admin/activities/${id}`;
window.editActivity = (id) => window.location.href = `/admin/activities/${id}/form`;