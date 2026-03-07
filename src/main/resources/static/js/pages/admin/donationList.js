import {donationApi} from '../../apis/donationApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {page: 1, size: 2, search: ''};

const elements = {
    tableBody: document.getElementById('donationTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

// 1. Hàm tiện ích format tiền tệ (Ví dụ: 5000000 -> 5.000.000 đ)
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
};

// 2. Hàm tiện ích lấy Badge Trạng thái theo giao diện
const getStatusBadge = (status) => {
    const styles = {
        'PENDING': {
            text: 'Chờ duyệt',
            class: 'bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-200 border-amber-200 dark:border-amber-800',
            dot: '<span class="h-1.5 w-1.5 rounded-full bg-amber-500 animate-pulse"></span>',
            rowClass: 'bg-amber-50/50 dark:bg-amber-900/10'
        },
        'CONFIRMED': {
            text: 'Đã xác nhận',
            class: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300',
            dot: '',
            rowClass: ''
        },
        'REJECTED': {
            text: 'Đã từ chối',
            class: 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300',
            dot: '',
            rowClass: 'opacity-75'
        },
        'FAILED': {
            text: 'Thất bại',
            class: 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300',
            dot: '',
            rowClass: 'opacity-75'
        }
    };
    return styles[status] || styles['PENDING'];
};

// 3. Hàm lấy Icon cho Phương thức thanh toán
const getPaymentMethodIcon = (method) => {
    const icons = {
        'CASH': {
            icon: 'payments',
            label: 'Tiền mặt'
        },
        'BANK_TRANSFER_ONLINE': {
            icon: 'account_balance',
            label: 'CK Online'
        },
        'BANK_TRANSFER_OFFLINE': {
            icon: 'receipt_long',
            label: 'Offline'
        }
    };
    return icons[method] || {icon: 'help_outline', label: method};
};

// 4. Hàm Render Bảng
const renderTable = (donations) => {
    if (!donations || donations.length === 0) {
        elements.tableBody.innerHTML = `<tr><td colspan="7" class="px-6 py-10 text-center text-slate-500">Chưa có dữ liệu quyên góp nào.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = donations.map(item => {
        const statusStyle = getStatusBadge(item.status);
        const payment = getPaymentMethodIcon(item.paymentMethod);
        const donatedAt = item.donatedAt ? new Date(item.donatedAt).toLocaleString('vi-VN', {
            hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric'
        }) : '---';

        return `
        <tr class="${statusStyle.rowClass} hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="text-sm font-mono text-slate-900 dark:text-white font-medium">#${item.memoCode || `ORD-${item.id}`}</span>
                <div class="text-xs text-slate-500 mt-0.5">${donatedAt}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm font-medium text-slate-900 dark:text-white">${item.donorName || 'Ẩn danh'}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right">
                <div class="text-sm font-bold ${item.status === 'REJECTED' ? 'text-slate-500 line-through' : 'text-slate-900 dark:text-white'}">
                    ${formatCurrency(item.amount)}
                </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-slate-700 dark:text-slate-300">${item.objectName || '---'}</div>
                <div class="text-xs text-slate-500">${item.type === 'EVENT' ? 'Sự kiện' : 'Hoạt động'}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap hidden xl:table-cell">
                <div class="flex items-center gap-1.5">
                    <span class="material-symbols-outlined text-[16px] text-slate-400">${payment.icon}</span>
                    <span class="text-sm text-slate-600 dark:text-slate-400">${payment.label}</span>
                </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold border ${statusStyle.class}">
                    ${statusStyle.dot}
                    ${statusStyle.text}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <div class="flex items-center justify-end gap-2">
                    ${item.status === 'PENDING_APPROVED' ? `
                        <button onclick="handleAction(${item.id}, 'REJECT')" class="text-red-600 hover:text-red-800 p-1.5 hover:bg-red-50 rounded-lg transition-colors" title="Từ chối">
                            <span class="material-symbols-outlined text-[20px]">close</span>
                        </button>
                        <button onclick="handleAction(${item.id}, 'CONFIRM')" class="bg-primary text-slate-900 hover:bg-primary/90 px-3 py-1.5 rounded-lg text-xs font-bold shadow-sm transition-colors flex items-center gap-1">
                            <span class="material-symbols-outlined text-[16px]">check</span> Duyệt
                        </button>
                    ` : `
                        <button onclick="viewDetail(${item.id})" class="text-slate-400 hover:text-primary p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors">
                            <span class="material-symbols-outlined text-[20px]">visibility</span>
                        </button>
                    `}
                </div>
            </td>
        </tr>
        `;
    }).join('');
};

// 5. Hàm Load dữ liệu chính
const loadDonations = async () => {
    try {
        const response = await donationApi.getDonations(state);
        const data = response.data
        console.log(data)
        renderTable(data.data);

        // Gọi render phân trang
        renderPagination(data, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadDonations();
        });
    } catch (error) {
        console.error("Lỗi khi tải danh sách quyên góp:", error);
    }
};

/**
 * Xử lý thay đổi trạng thái quyên góp (Duyệt/Từ chối)
 * @param {number} id - ID của khoản quyên góp
 * @param {string} action - 'CONFIRM' hoặc 'REJECT'
 */
window.handleAction = async (id, action) => {
    const isConfirm = action === 'CONFIRM';
    const statusText = isConfirm ? 'duyệt' : 'từ chối';
    const statusEnum = isConfirm ? 'CONFIRMED' : 'REJECTED';
    const confirmColor = isConfirm ? '#10b981' : '#ef4444'; // Xanh emerald hoặc Đỏ rose

    const message = `Bạn có chắc chắn muốn ${statusText} khoản quyên góp này không?`;
    if (!confirm(message)) return;

    try {
        const response = await donationApi.changeStatus(id, statusEnum);

        if (response.status === 200) {
            alert(response.message || 'Cập nhật thành công!');

            loadDonations();
        }
    } catch (error) {
        console.error("Lỗi cập nhật trạng thái:", error);
        alert(error.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái.');
    }
};

// Khởi chạy
document.addEventListener('DOMContentLoaded', loadDonations);