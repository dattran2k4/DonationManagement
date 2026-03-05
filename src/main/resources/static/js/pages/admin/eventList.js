import {eventApi} from '../../apis/eventApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {page: 1, size: 5, search: ''};

const elements = {
    tableBody: document.getElementById('eventTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

// Hàm tiện ích format tiền tệ rút gọn (VD: 650000000 -> 650tr)
const formatMoney = (amount) => {
    if (amount >= 1000000000) return (amount / 1000000000).toFixed(1).replace('.0', '') + ' tỷ';
    if (amount >= 1000000) return (amount / 1000000).toFixed(0) + 'tr';
    return amount.toLocaleString('vi-VN') + 'đ';
};

// Hàm tiện ích lấy style cho Badge Trạng thái
const getStatusBadge = (status) => {
    const styles = {
        'ONGOING': {
            text: 'Đang diễn ra',
            colorClass: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
            dotClass: 'bg-green-500'
        },
        'ENDED': {
            text: 'Đã kết thúc',
            colorClass: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
            dotClass: 'bg-blue-500'
        },
        'DRAFT': {
            text: 'Nháp',
            colorClass: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400',
            dotClass: 'bg-slate-400'
        },
        'UPCOMING': {
            text: 'Sắp diễn ra',
            colorClass: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400',
            dotClass: 'bg-amber-500'
        }
    };
    const s = styles[status] || styles['DRAFT'];
    return `
        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${s.colorClass}">
            <span class="w-1.5 h-1.5 rounded-full ${s.dotClass}"></span> ${s.text}
        </span>
    `;
};

// Hàm Render Bảng
const renderTable = (data) => {
    if (!data || data.length === 0) {
        elements.tableBody.innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-slate-500">Không tìm thấy sự kiện nào.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = data.map(item => {
        // Tính phần trăm gây quỹ
        const percent = item.targetAmount > 0 ? Math.min(Math.round((item.currentAmount / item.targetAmount) * 100), 100) : 0;
        const isCompleted = percent >= 100;

        return `
        <tr class="group hover:bg-slate-50 dark:hover:bg-white/5 transition-colors">
            <td class="px-6 py-4">
                <div class="h-10 w-10 rounded-lg bg-cover bg-center shadow-sm" 
                     style="background-image: url('${item.thumbnailUrl || '/static/images/default-event.png'}')"></div>
            </td>
            <td class="px-6 py-4">
                <div class="font-semibold text-slate-900 dark:text-slate-100">${item.name}</div>
                <div class="text-xs text-slate-500 mt-0.5">Mã: ${item.code || `EVT-${item.id}`}</div>
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
            <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-2">
                    <a href="/admin/events/${item.id}/form" class="text-slate-500 dark:text-slate-400 hover:text-primary dark:hover:text-primary p-1 rounded-md hover:bg-primary/10 transition-all group/btn" title="Cập nhật">
                        <span class="material-symbols-outlined text-[20px]">edit</span>
                    </a>
                </div>
            </td>
        </tr>
        `;
    }).join('');
};

const loadEvents = async () => {
    try {
        const response = await eventApi.getEvents(state);
        const data = response.data
        console.log(data)
        renderTable(data.data);

        // Gọi render phân trang
        renderPagination(data, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadEvents();
        });
    } catch (error) {
        console.error("Lỗi:", error);
    }
};

document.addEventListener('DOMContentLoaded', loadEvents);