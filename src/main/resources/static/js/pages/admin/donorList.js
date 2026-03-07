import {donorApi} from '../../apis/donorApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {page: 1, size: 10, search: '', type: ''};

const elements = {
    tableBody: document.getElementById('donorTableBody'),
    paginationContainer: document.getElementById('paginationContainer')
};

// 1. Hàm helper lấy chữ cái đầu của tên (Avatar cá nhân)
const getInitials = (name) => {
    return name ? name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) : 'NA';
};

// 2. Hàm helper định dạng tiền tệ
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' ₫';
};

// 3. Render Badge cho loại nhà hảo tâm
const getTypeBadge = (type) => {
    const isOrg = type === 'ORGANIZATION';
    const config = isOrg
        ? {text: 'Tổ chức', class: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300'}
        : {text: 'Cá nhân', class: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300'};

    return `<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.class}">${config.text}</span>`;
};

// 4. Hàm Render Row
const renderDonorRow = (donor) => {
    const isOrg = donor.type === 'ORGANIZATION';
    const orgInfo = donor.organization;
    const joinDate = donor.createdAt ? new Date(donor.createdAt).toLocaleDateString('vi-VN') : '---';

    // Avatar Logic: Nếu là tổ chức hiện icon tòa nhà, cá nhân hiện chữ cái đầu
    const avatarHtml = isOrg
        ? `<div class="h-10 w-10 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
             <span class="material-symbols-outlined text-[20px]">apartment</span>
           </div>`
        : `<div class="h-10 w-10 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold text-sm">
             ${getInitials(donor.fullName)}
           </div>`;

    return `
    <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors group">
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex items-center">
                <div class="h-10 w-10 shrink-0">${avatarHtml}</div>
                    <div class="ml-4">
                        ${isOrg && orgInfo ? `
                            <div class="text-sm font-semibold text-slate-900 dark:text-white">
                                ${orgInfo.name}
                            </div>
                            <div class="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5 flex items-center">
                                <span class="material-symbols-outlined text-[12px] mr-1">person_pin</span>
                                Đại diện: ${orgInfo.representative || '---'}
                            </div>
                    ` : `<div class="text-sm font-semibold text-slate-900 dark:text-white">${donor.fullName}</div>`}
                    </div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            ${getTypeBadge(donor.type)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex flex-col gap-1">
                <div class="flex items-center text-sm text-slate-900 dark:text-slate-200">
                    <span class="material-symbols-outlined text-[16px] mr-1.5 text-slate-400">call</span>
                    ${donor.phone || '---'}
                </div>
                <div class="flex items-center text-sm text-slate-500 dark:text-slate-400">
                    <span class="material-symbols-outlined text-[16px] mr-1.5 text-slate-400">mail</span>
                    ${donor.email || '---'}
                </div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-500 dark:text-slate-400">
            ${joinDate}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-900 dark:text-slate-200 text-center">
            ${donor.numberOfDonations || 0}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-right font-bold text-slate-900 dark:text-white">
            ${formatCurrency(donor.totalDonationAmount)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-center text-sm font-medium sticky right-0 bg-white dark:bg-slate-900 group-hover:bg-slate-50 dark:group-hover:bg-slate-800/50 shadow-[-10px_0_15px_-10px_rgba(0,0,0,0.1)] dark:shadow-[-10px_0_15px_-10px_rgba(0,0,0,0.5)] transition-colors">
            <div class="flex items-center justify-center gap-2">
                <button onclick="viewDonorProfile(${donor.id})" class="text-slate-400 hover:text-primary p-1 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" title="Xem hồ sơ">
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                <button onclick="editDonor(${donor.id})" class="text-slate-400 hover:text-blue-500 p-1 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" title="Chỉnh sửa">
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                </button>
                <button onclick="viewDonationHistory(${donor.id})" class="text-slate-400 hover:text-orange-500 p-1 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" title="Lịch sử quyên góp">
                    <span class="material-symbols-outlined text-[20px]">history</span>
                </button>
            </div>
        </td>
    </tr>`;
};

// 5. Hàm Load dữ liệu
const loadDonors = async () => {
    try {
        const apiParams = {...state, page: state.page - 1};
        const response = await donorApi.getAllDonors(apiParams);

        const pageData = response.data; // Giả sử BE trả về Page object trong field data
        const donors = pageData.data || [];

        if (donors.length === 0) {
            elements.tableBody.innerHTML = `<tr><td colspan="7" class="px-6 py-10 text-center text-slate-500">Không tìm thấy nhà hảo tâm nào</td></tr>`;
        } else {
            elements.tableBody.innerHTML = donors.map(d => renderDonorRow(d)).join('');
        }

        renderPagination({
            page: pageData.number + 1,
            pageSize: pageData.size,
            totalPages: pageData.totalPages,
            totalItems: pageData.totalItems
        }, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadDonors();
        });
    } catch (error) {
        console.error("Error loading donors:", error);
    }
};

// Khởi chạy
document.addEventListener('DOMContentLoaded', () => {
    loadDonors();
});

// Gắn các hàm hành động vào window để HTML onclick gọi được
window.viewDonorProfile = (id) => console.log('Xem hồ sơ:', id);
window.editDonor = (id) => console.log('Sửa:', id);
window.viewDonationHistory = (id) => console.log('Lịch sử:', id);